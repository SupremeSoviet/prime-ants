"""Read-only native NBT/Anvil boundary audit for the explicit development harness.

No world writes, migration, normalization, inventory sorting or tolerance. Object
key order is immaterial; every value and list order is retained. Raw files have
independent byte hashes, including native numeric tag representations.
"""
import gzip,hashlib,io,json,math,struct,uuid,zlib
from pathlib import Path

def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def tree(p):return {f.relative_to(p).as_posix():sha(f) for f in sorted(p.rglob('*')) if f.is_file()}

class NBT:
    def __init__(self,data):self.s=io.BytesIO(data)
    def number(self,fmt):
        n=struct.calcsize(fmt);b=self.s.read(n)
        if len(b)!=n:raise ValueError('Truncated native NBT')
        return struct.unpack('>'+fmt,b)[0]
    def string(self):return self.s.read(self.number('H')).decode('utf-8')
    def value(self,t):
        if t in range(1,7):return self.number({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[t])
        if t==7:return [self.number('b') for _ in range(self.number('i'))]
        if t==8:return self.string()
        if t==9:
            kind=self.number('B');count=self.number('i')
            if count<0 or kind==0 and count:raise ValueError('Invalid native list length/type')
            values=[self.value(kind) for _ in range(count)]
            # Minecraft26.3 ListTag.loadList calls addAndUnwrap once per element.
            return [v[''] if isinstance(v,dict) and set(v)=={''} else v for v in values]
        if t==10:
            result={}
            while True:
                kind=self.number('B')
                if kind==0:return result
                name=self.string()
                if name in result:raise ValueError('Duplicate native NBT compound key')
                result[name]=self.value(kind)
        if t in (11,12):return [self.number('i' if t==11 else 'q') for _ in range(self.number('i'))]
        raise ValueError('Unsupported native NBT tag '+str(t))
    def root(self):
        kind=self.number('B');assert kind==10;self.string();value=self.value(kind)
        assert not self.s.read(1),'Trailing native NBT data';return value

def dat(p):
    b=p.read_bytes();return NBT(gzip.decompress(b) if b[:2]==b'\x1f\x8b' else b).root()

def region(p):
    raw=p.read_bytes();assert len(raw)>=8192
    for i in range(1024):
        entry=int.from_bytes(raw[i*4:i*4+4],'big');offset=entry>>8;sectors=entry&255
        if offset==0:continue
        start=offset*4096;n=int.from_bytes(raw[start:start+4],'big');compression=raw[start+4]
        assert offset>=2 and n>0 and n<=sectors*4096-4
        if compression&128:raise ValueError('External region entry requires explicit audit: '+str(p))
        content=raw[start+5:start+4+n]
        decoder={1:gzip.decompress,2:zlib.decompress,3:lambda x:x}.get(compression)
        if decoder is None:raise ValueError('Unsupported region compression '+str(compression))
        yield NBT(decoder(content)).root()

def actor_id(a):return str(uuid.UUID(bytes=b''.join(struct.pack('>i',n) for n in a['UUID'])))

def capture(world,footprint,format):
    world=Path(world).resolve();dimension=world/'dimensions/prime_ants_test/placement_soil'
    result=dict(actors={},blocks={},tables={},tableRoots={},terrain={},clocks={},native_files=tree(world))
    for p in sorted((dimension/'entities').glob('*.mca')):
        for chunk in region(p):
            for a in chunk.get('Entities',[]):
                if a['id'] not in ('prime_ants:lasius_niger_queen','prime_ants:lasius_niger_worker','minecraft:item'):
                    # Discover the registered production entity IDs from complete actor NBT,
                    # rather than accepting unrelated vanilla population as a colony member.
                    if 'AntForm' not in a:continue
                key=actor_id(a);assert key not in result['actors'];result['actors'][key]=a
    chunks={}
    for p in sorted((dimension/'region').glob('*.mca')):
        for chunk in region(p):
            cp=(chunk['xPos'],chunk['zPos']);assert cp not in chunks;chunks[cp]=chunk
            for b in chunk.get('block_entities',[]):
                key=f"{b['x']}, {b['y']}, {b['z']}"
                if key in footprint:result['blocks'][key]=b
    for key in footprint:
        x,y,z=map(int,key.split(', '));assert format['minY']<=y<=format['maxY'],('Virtual coordinate is not a persisted cell',key);chunk=chunks[(x//16,z//16)];sections=[s for s in chunk['sections'] if s['Y']==y//16];assert len(sections)==1
        states=sections[0]['block_states'];palette=states['palette'];data=states.get('data',[])
        if len(palette)==1:index=0
        else:
            bits=max(4,(len(palette)-1).bit_length());per=64//bits;n=(y%16)*256+(z%16)*16+x%16
            index=((data[n//per]&((1<<64)-1))>>((n%per)*bits))&((1<<bits)-1)
        raw=palette[index]
        if isinstance(raw,str):
            assert raw in format['blockDefaults'],('Unknown native default block',raw)
            result['terrain'][key]=format['blockDefaults'][raw]
        else:
            assert isinstance(raw,dict) and 'id' in raw and set(raw)<={'id','properties'},raw
            base=format['blockDefaults'][raw['id']]
            value=dict(base)
            if 'properties' in base:value['properties']=dict(base['properties'])|raw.get('properties',{})
            else:assert not raw.get('properties')
            result['terrain'][key]=value
    for p in sorted((dimension/'data/prime_ants').glob('*.dat')):
        value=dat(p);result['tableRoots']['prime_ants:'+p.stem]=value;result['tables']['prime_ants:'+p.stem]=value['data']
    result['clocks']['savedGameTime']=dat(world/'level.dat')['Data']['Time']
    result['clocks']['nurseries']={k:{n:v[n] for n in ('LoadedTicks','LastSimulationGameTime','LastOrdinaryGameTime','Catchup') if n in v} for k,v in result['blocks'].items() if 'Brood' in v}
    assert result['actors'] and result['blocks'] and result['tables'] and result['terrain']
    return result
