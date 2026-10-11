"""Offline rejection fixtures. Real multi-process execution is the separate checkServerSmoke task."""
import importlib.util
import contextlib
import io
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import time
import unittest
from unittest.mock import patch
import xml.etree.ElementTree as ET

spec=importlib.util.spec_from_file_location('harness',Path(__file__).with_name('gametest-harness.py'))
h=importlib.util.module_from_spec(spec);spec.loader.exec_module(h)

class HarnessTests(unittest.TestCase):
    def test_fast_guard_cli_keeps_full_packet_with_bounded_console_summary(self):
        # Regression for a completed guard blocked writing >4 KiB through Gradle's inherited pipe.
        with tempfile.TemporaryDirectory() as temporary:
            folder=Path(temporary);packet={'passed':True,'issues':[],'unit_count':121,'model_count':26,
                'reports':{'report-'+str(i):{'sha256':'a'*64,'mtime':1} for i in range(100)}}
            def guard(destination,since):
                h.write(destination/'fast-guards.json',packet);return packet
            output=io.StringIO()
            with patch.object(h,'fast_guards',side_effect=guard),patch.object(sys,'argv',['harness','fast-guards','--folder',str(folder),'--since','0']),contextlib.redirect_stdout(output):
                h.main()
            self.assertEqual(packet,h.read(folder/'fast-guards.json'))
            self.assertLess(len(output.getvalue().encode('utf-8')),4096)
            self.assertEqual({'passed':True,'issues':[],'unit_count':121,'model_count':26},json.loads(output.getvalue()))

    def diagnostic_cli_fixture(self,label,log_text,launcher_exit=0,failed=False,log_mode='supplied'):
        """Fresh offline XML/logs through the real collector and CLI in a child Python.

        Only executable-input/correspondence dependencies are isolated. Optional
        archival is additive; these synthetic packets are never gameplay evidence.
        """
        archive=os.environ.get('PRIME_ANTS_DIAGNOSTIC_CLI_EVIDENCE')
        with tempfile.TemporaryDirectory(prefix='offline-diagnostic-cli-') as temporary:
            root=Path(archive)/label if archive else Path(temporary)/label
            root.mkdir(parents=True,exist_ok=False)
            now=time.time()
            h.write(root/'frozen.json',dict(label='fresh offline collector/CLI fixture; not gameplay evidence',
                captured_epoch=now-20,expected_names=['offline_cli_pass','offline_cli_row'],correspondence={},launch={},inputs={}))
            report=self.report(root,names=('offline_cli_pass','offline_cli_row'),tag='failure' if failed else None)
            original=report.read_bytes();native=root/'native.xml';report.rename(native)
            if failed:
                xml=ET.parse(native);xml.getroot().set('failures','1');xml.write(native,encoding='utf-8')
                original=native.read_bytes()
            log=root/'native.log';log_bytes=log_text.encode('utf-8');log.write_bytes(log_bytes)
            invocation=root/'invocation.json'
            h.write(invocation,dict(label='offline fixture; no server launched',start_epoch=now-10,
                end_epoch=time.time()+10,exit_code=launcher_exit))
            bootstrap="""import importlib.util, sys
from pathlib import Path
from unittest.mock import patch
script=Path(sys.argv[1]); args=sys.argv[2:]
spec=importlib.util.spec_from_file_location('actual_diagnostic_cli',script)
d=importlib.util.module_from_spec(spec);spec.loader.exec_module(d)
sys.argv=[str(script),*args]
with patch.object(d,'correspondence',return_value={}),patch.object(d.h,'inputs',return_value={}):
    d.main()
"""
            command=[sys.executable,'-c',bootstrap,str(Path(__file__).with_name('diagnostic-gametest.py')),
                'collect','--folder',str(root),'--report',str(native),'--invocation',str(invocation)]
            if log_mode!='omitted':
                evidence=log if log_mode=='supplied' else root/('absent.log' if log_mode=='missing' else 'unreadable.log')
                if log_mode=='unreadable':evidence.mkdir()
                command += ['--log',str(evidence)]
            started=time.time();process=subprocess.run(command,capture_output=True,text=True,encoding='utf-8')
            (root/'cli.stdout.txt').write_text(process.stdout,encoding='utf-8')
            (root/'cli.stderr.txt').write_text(process.stderr,encoding='utf-8')
            result=h.read(root/'validation.json')
            h.write(root/'cli-process.json',dict(label='offline real collector and main entry point; not gameplay evidence',
                command=command,start_epoch=started,end_epoch=time.time(),exit_code=process.returncode,
                collector_sha256=h.sha(Path(__file__).with_name('diagnostic-gametest.py')),
                harness_tests_sha256=h.sha(__file__),native_xml_unchanged=native.read_bytes()==original,
                archived_xml_identical=(root/'server.xml').read_bytes()==original,
                native_log_unchanged=log.read_bytes()==log_bytes))
            self.assertEqual(original,native.read_bytes());self.assertEqual(original,(root/'server.xml').read_bytes())
            self.assertEqual(log_bytes,log.read_bytes());self.assertEqual('',process.stderr)
            return result,process.returncode

    def diagnostic_log(self):
        return '> Task :deleteGameTestRunDir SKIPPED\n> Task :acceptGameTestEula SKIPPED\n2 GAME TESTS COMPLETE\nGame test server shutting down\n'

    def test_diagnostic_cli_rejects_omitted_log_for_pass_and_archived_negative_categories(self):
        good=self.diagnostic_log()
        negatives=[('green',good),('truncated',good.split('2 GAME TESTS COMPLETE')[0]),
            ('wrapped-native-three',good+"Process 'command 'C:\\offline\\java.exe'' finished with non-zero exit\r\n  value 3\n"),
            ('missing-guards',good.replace('> Task :deleteGameTestRunDir SKIPPED\n','').replace('> Task :acceptGameTestEula SKIPPED\n',''))]
        for label,text in negatives:
            with self.subTest(label=label):
                result,code=self.diagnostic_cli_fixture('omitted-'+label,text,log_mode='omitted')
                self.assertFalse(result['infrastructure_valid']);self.assertTrue(result['gameplay_passed']);self.assertEqual(1,code)
                self.assertIn('missing log evidence: --log omitted',result['infrastructure_issues'])
                self.assertIsNone(result['native_exit_code'])

    def test_diagnostic_cli_omitted_log_preserves_xml_gameplay_red(self):
        result,code=self.diagnostic_cli_fixture('omitted-gameplay-red',self.diagnostic_log(),1,failed=True,log_mode='omitted')
        self.assertFalse(result['infrastructure_valid']);self.assertFalse(result['gameplay_passed']);self.assertEqual(1,code)
        self.assertEqual(1,len(result['failures']));self.assertIsNone(result['native_exit_code'])
        self.assertIn('missing log evidence: --log omitted',result['infrastructure_issues'])

    def test_diagnostic_cli_records_explicit_missing_log(self):
        result,code=self.diagnostic_cli_fixture('explicit-missing',self.diagnostic_log(),log_mode='missing')
        self.assertFalse(result['infrastructure_valid']);self.assertTrue(result['gameplay_passed']);self.assertEqual(1,code)
        self.assertTrue(any(i.startswith('missing log evidence: unreadable') for i in result['infrastructure_issues']))
        self.assertIsNone(result['native_exit_code'])

    def test_diagnostic_cli_records_explicit_unreadable_log(self):
        # A directory is an OS-level unreadable text file on every supported platform.
        result,code=self.diagnostic_cli_fixture('explicit-unreadable',self.diagnostic_log(),log_mode='unreadable')
        self.assertFalse(result['infrastructure_valid']);self.assertTrue(result['gameplay_passed']);self.assertEqual(1,code)
        self.assertTrue(any(i.startswith('missing log evidence: unreadable') for i in result['infrastructure_issues']))
        self.assertIsNone(result['native_exit_code'])

    def test_diagnostic_cli_accepts_only_valid_passing_packet(self):
        result,code=self.diagnostic_cli_fixture('valid-green',self.diagnostic_log())
        self.assertTrue(result['infrastructure_valid']);self.assertTrue(result['gameplay_passed']);self.assertEqual(0,code)
        self.assertTrue(all(result['protection'].values()));self.assertEqual(0,result['native_exit_code'])

    def test_diagnostic_cli_rejects_truncated_or_missing_completion_markers(self):
        good=self.diagnostic_log()
        for label,text in [('truncated',good.split('2 GAME TESTS COMPLETE')[0]),
                           ('missing-completion',good.replace('2 GAME TESTS COMPLETE\n','')),
                           ('missing-shutdown',good.replace('Game test server shutting down\n',''))]:
            with self.subTest(label=label):
                result,code=self.diagnostic_cli_fixture(label,text)
                self.assertFalse(result['infrastructure_valid']);self.assertTrue(result['gameplay_passed']);self.assertEqual(1,code)
                self.assertIn('native completion/shutdown missing',result['infrastructure_issues'])

    def test_diagnostic_cli_rejects_native_three_including_powershell_wrapping(self):
        for label,gap,launcher in [('native-three',' ',0),('native-three-wrapped','\r\n  ',0),
                                   ('native-three-launcher-three','\r\n  ',3)]:
            with self.subTest(label=label):
                text=self.diagnostic_log()+"Process 'command 'C:\\offline\\java.exe'' finished with non-zero exit"+gap+'value 3\n'
                result,code=self.diagnostic_cli_fixture(label,text,launcher)
                self.assertFalse(result['infrastructure_valid']);self.assertTrue(result['gameplay_passed']);self.assertEqual(1,code)
                self.assertEqual(3,result['native_exit_code']);self.assertIn('native exit disagrees with report',result['infrastructure_issues'])

    def test_diagnostic_cli_rejects_each_absent_or_unskipped_client_guard(self):
        for task in ('deleteGameTestRunDir','acceptGameTestEula'):
            for state in ('absent','UP-TO-DATE'):
                with self.subTest(task=task,state=state):
                    line='> Task :'+task+' SKIPPED\n'
                    text=self.diagnostic_log().replace(line,'' if state=='absent' else line.replace('SKIPPED',state))
                    result,code=self.diagnostic_cli_fixture(task+'-'+state,text)
                    self.assertFalse(result['infrastructure_valid']);self.assertTrue(result['gameplay_passed']);self.assertEqual(1,code)
                    self.assertFalse(result['protection'][task]);self.assertIn('client guard not SKIPPED: '+task,result['infrastructure_issues'])

    def test_diagnostic_cli_preserves_explained_gameplay_red_and_exit_one(self):
        text=self.diagnostic_log()+"Process 'command 'C:\\offline\\java.exe'' finished with non-zero exit\r\n  value 1\n"
        result,code=self.diagnostic_cli_fixture('valid-gameplay-red',text,1,failed=True)
        self.assertTrue(result['infrastructure_valid']);self.assertFalse(result['gameplay_passed']);self.assertEqual(1,code)
        self.assertEqual(1,result['native_exit_code']);self.assertEqual(1,len(result['failures']))

    def test_pinned_fabric_names_preserve_digit_boundaries_and_ordinary_camel_case(self):
        for source, expected in [('copy1ActualCarrierDeath','copy1actual_carrier_death'),
                                 ('copy2ExactPair','copy2exact_pair'),('ordinaryCamelCase','ordinary_camel_case'),
                                 ('SurfaceAccessGameTest','surface_access_game_test'),('tier2PaidTarget','tier2paid_target')]:
            self.assertEqual(expected,h.snake(source))

    def test_diagnostic_freeze_rejects_wrong_names_changed_inputs_body_and_bounds(self):
        spec=importlib.util.spec_from_file_location('diagnostic',Path(__file__).with_name('diagnostic-gametest.py'))
        d=importlib.util.module_from_spec(spec);spec.loader.exec_module(d)
        mapping={'a':dict(method='a',mode='SAFE',body_sha256='body',common_body_sha256='common',annotation='maxTicks=12000')}
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);freeze=dict(captured_epoch=time.time()-20,expected_names=['a','b'],correspondence=mapping,launch={},inputs={'compiled':'frozen'})
            h.write(root/'frozen.json',freeze);report=self.report(root,['a','b']);end=time.time()+10
            with patch.object(d,'correspondence',return_value=mapping),patch.object(d.h,'inputs',return_value=freeze['inputs']):
                self.assertTrue(d.collect(root,report,time.time()-10,end,0)['gameplay_passed'])
                report=self.report(root,['a','copy1actual']);result=d.collect(root,report,time.time()-10,end,0)
                self.assertFalse(result['infrastructure_valid']);self.assertTrue(any('missing/unexpected' in i for i in result['issues']))
            report=self.report(root,['a','b'])
            with patch.object(d,'correspondence',return_value={'a':dict(mapping['a'],annotation='maxTicks=12001')}),patch.object(d.h,'inputs',return_value={'compiled':'changed'}):
                result=d.collect(root,report,time.time()-10,end,0)
                self.assertFalse(result['infrastructure_valid']);self.assertIn('executable inputs changed during execution',result['issues']);self.assertIn('registered method/mode/body/bounds changed',result['issues'])

    def test_diagnostic_identity_success_keeps_gameplay_failure_red(self):
        spec=importlib.util.spec_from_file_location('diagnostic',Path(__file__).with_name('diagnostic-gametest.py'))
        d=importlib.util.module_from_spec(spec);spec.loader.exec_module(d)
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);h.write(root/'frozen.json',dict(captured_epoch=time.time()-20,expected_names=['a','b'],correspondence={},launch={},inputs={}))
            report=self.report(root,tag='failure');xml=ET.parse(report);xml.getroot().set('failures','1');xml.write(report)
            with patch.object(d,'correspondence',return_value={}),patch.object(d.h,'inputs',return_value={}):
                log=root/'native.log';log.write_text(self.diagnostic_log()+"Process 'command 'C:\\offline\\java.exe'' finished with non-zero exit value 1\n",encoding='utf-8')
                result=d.collect(root,report,time.time()-10,time.time()+10,1,log)
            self.assertTrue(result['infrastructure_valid']);self.assertFalse(result['gameplay_passed']);self.assertEqual(1,len(result['failures']))

    def test_copy_freeze_requires_three_identical_modes_bodies_and_annotation_bounds(self):
        spec=importlib.util.spec_from_file_location('diagnostic',Path(__file__).with_name('diagnostic-gametest.py'))
        d=importlib.util.module_from_spec(spec);spec.loader.exec_module(d)
        original=dict(source='fixture.java',method='ordinaryBody',max_ticks=12000,annotation='maxTicks=12000',mode='SAFE',body_sha256='same',common_body_sha256='same common')
        mapping={'original':original,'copy1':dict(original,method='copy1OrdinaryBody'),'copy2':dict(original,method='copy2OrdinaryBody')}
        self.assertEqual(1,len(d.copy_equivalence(mapping)))
        self.assertEqual(d.copy_equivalence(mapping),d.copy_equivalence(json.loads(json.dumps(mapping,sort_keys=True))))
        with self.assertRaises(ValueError):d.copy_equivalence({k:v for k,v in mapping.items() if k!='copy2'})
        for field in ('max_ticks','annotation','mode','body_sha256','common_body_sha256'):
            wrong={**mapping,'copy2':dict(mapping['copy2'],**{field:'different'})}
            with self.assertRaises(ValueError):d.copy_equivalence(wrong)

    def test_native_exit_preserves_wrapped_gradle_failure_and_requires_completion(self):
        spec=importlib.util.spec_from_file_location('diagnostic',Path(__file__).with_name('diagnostic-gametest.py'))
        d=importlib.util.module_from_spec(spec);spec.loader.exec_module(d)
        good='6 GAME TESTS COMPLETE\nGame test server shutting down'
        for gap in (' ',' \n','\r\n  '):
            text=good+"\nProcess 'command 'C:\\jdk\\bin\\java.exe'' finished with non-zero exit"+gap+'value 1'
            self.assertEqual(1,d.native_exit_code(text))
        self.assertEqual(0,d.native_exit_code(good));self.assertIsNone(d.native_exit_code('partial server startup'))

    def test_both_client_tasks_exclude_legacy_and_sharded_server_graphs(self):
        source=(h.ROOT/'build.gradle').read_text(encoding='utf-8')
        guard="onlyIf { !gradle.taskGraph.hasTask(':runGameTest') && !gradle.taskGraph.hasTask(':prepareGameTestServers') }"
        for task in ('deleteGameTestRunDir','acceptGameTestEula'):
            self.assertIn("tasks.named('"+task+"') { "+guard+" }",source)

    def report(self,folder,names=('a','b'),tag=None):
        root=ET.Element('testsuite',tests=str(len(names)),failures='0',errors='0',skipped='0')
        for name in names: ET.SubElement(root,'testcase',name=name,classname='offline fixture')
        if tag: ET.SubElement(next(root.iter('testcase')),tag,message='preserved negative')
        path=Path(folder)/'server.xml';ET.ElementTree(root).write(path,encoding='utf-8');return path

    def test_exact_fresh_report_and_failed_child_exit(self):
        with tempfile.TemporaryDirectory() as folder:
            p=self.report(folder);now=time.time()
            self.assertEqual([],h.validate_report(p,['a','b'],now-10,now+10,0)[1])
            self.assertIn('failed child exit: 7',h.validate_report(p,['a','b'],now-10,now+10,7)[1])

    def test_missing_unexpected_and_duplicate_execution_and_assignment(self):
        with tempfile.TemporaryDirectory() as folder:
            now=time.time()
            for names in [('a',),('a','b','c'),('a','a')]:
                p=self.report(folder,names);self.assertTrue(h.validate_report(p,['a','b'],now-10,now+10,0)[1])
            p=self.report(folder);self.assertIn('duplicate assignment',h.validate_report(p,['a','a'],now-10,now+10,0)[1])
            root=ET.parse(p);next(root.iter('testcase')).attrib.pop('name');root.write(p)
            self.assertIn('missing executed name',h.validate_report(p,['a','b'],now-10,now+10,0)[1])

    def wrappers(self):
        return [('wrong-tests',dict(tests='3')),('errors',dict(errors='1')),('skipped',dict(skipped='1')),
                ('failures',dict(failures='1')),('consistent-zero',dict(tests='2',errors='0',skipped='0',failures='0')),
                ('empty-zero',dict(tests='0',errors='0',skipped='0',failures='0')),('failing-child',dict(tests='2',failures='1'))]

    def wrapper(self,path,label,attrs,names=('a','b')):
        root=ET.Element('testsuites',**attrs)
        suite=ET.SubElement(root,'testsuite',tests=str(len(names)),failures='0',errors='0',skipped='0')
        for name in names:ET.SubElement(suite,'testcase',name=name,classname='offline fixture')
        if label=='failing-child':ET.SubElement(next(suite.iter('testcase')),'failure',message='preserved negative')
        ET.ElementTree(root).write(path,encoding='utf-8')

    def test_wrappers_rejected_before_case_extraction(self):
        with tempfile.TemporaryDirectory() as folder:
            for label,attrs in self.wrappers():
                with self.subTest(label=label):
                    p=Path(folder)/(label+'.xml');self.wrapper(p,label,attrs,() if label=='empty-zero' else ('a','b'))
                    original=p.read_bytes();cases,issues=h.validate_report(p,['a','b'],time.time()-10,time.time()+10,0)
                    self.assertEqual([],cases);self.assertTrue(any('expected testsuite' in i for i in issues))
                    self.assertEqual(original,p.read_bytes())

    def test_unit_and_model_guards_reject_every_wrapper(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);unit=root/'build/test-results/test/TEST-offline.xml';unit.parent.mkdir(parents=True)
            model=root/'build/test-results/client-model/headless.xml';model.parent.mkdir(parents=True)
            h.write(root/'frozen.json',dict(unit_cases=[['offline fixture','a'],['offline fixture','b']],model_cases=[['offline fixture','a'],['offline fixture','b']]))
            for kind,path in [('unit',unit),('model',model)]:
                for label,attrs in self.wrappers():
                    with self.subTest(kind=kind,label=label):
                        for good in [unit,model]:
                            fixture=self.report(folder);good.write_bytes(fixture.read_bytes())
                        self.wrapper(path,label,attrs,() if label=='empty-zero' else ('a','b'))
                        # Settle the known NTFS epoch rounding before testing root policy.
                        h.written_report_end(path)
                        with patch.object(h,'ROOT',root):result=h.fast_guards(root,time.time()-10)
                        self.assertFalse(result['passed']);self.assertEqual(0,result[kind+'_count'])
                        self.assertTrue(any('expected testsuite' in i for i in result['issues']))
            for good in [unit,model]:
                good.write_bytes(self.report(folder).read_bytes());h.written_report_end(good)
            with patch.object(h,'ROOT',root):self.assertTrue(h.fast_guards(root,time.time()-10)['passed'])

    def test_wrapper_rejection_survives_suite_and_successful_case_only_complete_merge(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);freeze=dict(assignments={'fast':[dict(index=0,names=['a','b'])]},concurrency=1,
                long_scope='complete long suite',prepared_epoch=time.time()-10,inventory=[dict(name='a'),dict(name='b')],launch={},inputs={})
            h.write(root/'frozen.json',freeze)
            def rejected_child(path,suite,assignment,frozen):
                path.mkdir();p=path/'server.xml';self.wrapper(p,'errors',dict(errors='1'))
                cases,issues=h.validate_report(p,assignment['names'],time.time()-10,time.time()+10,0)
                return dict(index=0,assigned=assignment['names'],start_epoch=time.time()-10,end_epoch=time.time()+10,
                    exit_code=0,passed=not issues,issues=issues,executed=len(cases),report_sha256=h.sha(p))
            with patch.object(h,'child',side_effect=rejected_child):summary=h.run_suite(root,'fast')
            self.assertFalse(summary['passed']);self.assertTrue(summary['union_issues'])
            original=(root/'fast-0/server.xml').read_bytes()
            # Even an independently successful case-only replacement cannot erase the child's rejection.
            good=self.report(folder);cases=list(ET.parse(good).getroot().iter('testcase'));h.merged(root/'fast.xml',cases)
            midpoint=(summary['start_epoch']+summary['end_epoch'])/2
            os.utime(root/'fast.xml',(midpoint,midpoint))
            h.merged(root/'long.xml',[])
            h.write(root/'long.json',dict(passed=True,shards=[],assigned=[],start_epoch=time.time()-10,end_epoch=time.time()+10))
            with patch.object(h,'inputs',return_value={}),patch.object(h,'ROOT',root):result=h.merge_complete(root)
            self.assertEqual([],h.validate_report(root/'server.xml',['a','b'],time.time()-10,time.time()+10,0)[1])
            self.assertFalse(result['passed']);self.assertTrue(any('expected testsuite' in i for i in result['issues']))
            self.assertFalse(any('stale' in i for i in result['issues']))
            self.assertEqual(original,(root/'fast-0/server.xml').read_bytes())
            self.assertEqual(summary,h.read(root/'fast.json'))

    def test_stale_missing_malformed_and_incomplete_reports(self):
        with tempfile.TemporaryDirectory() as folder:
            p=self.report(folder);now=time.time();os.utime(p,(1,1))
            self.assertIn('stale report',h.validate_report(p,['a','b'],now-10,now+10,0)[1])
            p.write_text('<testsuite><testcase',encoding='utf-8')
            self.assertTrue(any('malformed' in x for x in h.validate_report(p,['a','b'],now-10,now+10,0)[1]))
            p.unlink();self.assertTrue(h.validate_report(p,['a','b'],now-10,now+10,0)[1])
            p=self.report(folder);r=ET.parse(p);r.getroot().set('tests','3');r.write(p)
            self.assertIn('incomplete XML totals',h.validate_report(p,['a','b'],now-10,now+10,0)[1])

    def test_merge_retains_failures_errors_skips_and_duplicate_rows(self):
        with tempfile.TemporaryDirectory() as folder:
            for tag in ('failure','error','skipped'):
                p=self.report(folder,tag=tag);cases=list(ET.parse(p).getroot().iter('testcase'));out=Path(folder)/'merged.xml'
                self.assertIn('report '+tag,h.validate_report(p,['a','b'],time.time()-10,time.time()+10,0)[1])
                h.merged(out,cases+cases);r=ET.parse(out)
                self.assertEqual(4,len(list(r.iter('testcase'))));self.assertEqual(2,len(list(r.iter(tag))))
                self.assertTrue(h.validate_report(out,['a','b'],time.time()-10,time.time()+10,0)[1])

    def test_partition_rejects_omitted_duplicate_unknown_and_unclassified_names(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);resources=root/'src/gametest/resources';resources.mkdir(parents=True)
            source=root/'src/gametest/java/x/TinyGameTest.java';source.parent.mkdir(parents=True)
            source.write_text('@GameTest public void one(GameTestHelper c) {}\n@GameTest(maxTicks=4000) public void two(GameTestHelper c) {}',encoding='utf-8')
            (resources/'fabric.mod.json').write_text(json.dumps({'entrypoints':{'fabric-gametest':['x.TinyGameTest']}}),encoding='utf-8')
            config=root/'config/gametest-suites.json';config.parent.mkdir()
            rows=[dict(name=n,max_ticks=v['max_ticks'],suite='fast' if n.endswith('one') else 'long',weight_ticks=v['max_ticks'],reason='offline tick-path fixture') for n,v in h.discover(root).items()]
            config.write_text(json.dumps({'cases':rows}),encoding='utf-8');self.assertEqual(2,len(h.inventory(root)))
            for bad in [rows[:1],rows+[rows[0]],rows+[dict(rows[0],name='unknown')],[dict(rows[0],suite='unknown'),rows[1]]]:
                config.write_text(json.dumps({'cases':bad}),encoding='utf-8')
                with self.assertRaises(ValueError):h.inventory(root)

    def test_declared_weight_shards_are_deterministic_disjoint_and_complete(self):
        rows=[dict(name=str(i),weight_ticks=w) for i,w in enumerate([60000,60000,48000,30000,20000,18000,3000])]
        a=h.balance(rows,4);self.assertEqual(a,h.balance(list(reversed(rows)),4))
        names=[n for shard in a for n in shard['names']];self.assertEqual(len(names),len(set(names)));self.assertEqual(set(names),{r['name'] for r in rows})

    def test_real_ntfs_merge_write_has_a_strict_consistent_clock_interval(self):
        with tempfile.TemporaryDirectory() as folder:
            cases=[ET.Element('testcase',name='a',classname='offline')]
            for i in range(100):
                path=Path(folder)/(str(i)+'.xml');start=time.time();h.merged(path,cases)
                self.assertEqual([],h.validate_report(path,['a'],start-0.001,h.written_report_end(path),0)[1])
            os.utime(path,(1,1))
            self.assertIn('stale report',h.validate_report(path,['a'],start,h.written_report_end(path),0)[1])

    def test_archived_complete_gate_rejects_wrappers_for_server_unit_and_model(self):
        spec=importlib.util.spec_from_file_location('performance',Path(__file__).with_name('sample-performance.py'))
        performance=importlib.util.module_from_spec(spec);spec.loader.exec_module(performance)
        required=(h.unit_cases(),set(h.discover()),h.model_cases())
        with tempfile.TemporaryDirectory() as folder,patch.object(performance,'inputs',return_value={'offline':'fixture'}),patch.object(performance,'required_cases',return_value=required),contextlib.redirect_stdout(io.StringIO()):
            out=Path(folder)/'gate';performance.test_gate(out);results=h.read(out/'results.json')
        wrappers=[r for r in results if r['fixture'].startswith('wrapper-')]
        self.assertEqual(21,len(wrappers));self.assertTrue(all(r['result']=='rejected' and 'expected testsuite' in r['reason'] for r in wrappers))

if __name__=='__main__':unittest.main(verbosity=2)
