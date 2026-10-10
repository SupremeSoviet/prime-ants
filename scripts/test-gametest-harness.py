"""Offline rejection fixtures. Real multi-process execution is the separate checkServerSmoke task."""
import importlib.util
import json
import os
from pathlib import Path
import tempfile
import time
import unittest
import xml.etree.ElementTree as ET

spec=importlib.util.spec_from_file_location('harness',Path(__file__).with_name('gametest-harness.py'))
h=importlib.util.module_from_spec(spec);spec.loader.exec_module(h)

class HarnessTests(unittest.TestCase):
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

if __name__=='__main__':unittest.main(verbosity=2)
