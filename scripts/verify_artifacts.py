#!/usr/bin/env python3
"""Inspect distributables without printing config, tokens or signing material."""
import argparse
import hashlib
import json
import re
from pathlib import Path
import struct
import subprocess
import zipfile


def verify_native(archive):
    libraries=[]
    with zipfile.ZipFile(archive) as z:
        for name in z.namelist():
            if not name.endswith('.so'):
                continue
            data=z.read(name)
            if data[:4]!=b'\x7fELF' or data[4] not in (1,2) or data[5] not in (1,2):
                raise ValueError(f'Invalid native library: {name}')
            # 16 KB page-size compatibility is required on 64-bit Android ABIs.
            # Keep 32-bit ABIs at their 4 KB ELF requirement; rejecting armeabi-v7a
            # would incorrectly discard supported 32-bit handsets.
            parts=name.split('/')
            if len(parts)<3 or parts[-3]!='lib' or parts[-2] not in ('arm64-v8a','x86_64','armeabi-v7a','x86'):
                raise ValueError(f'Unexpected native library packaging: {name}')
            abi=parts[-2]
            required_alignment=16384 if abi in ('arm64-v8a','x86_64') else 4096
            endian='<' if data[5]==1 else '>'
            wide=data[4]==2
            offset=struct.unpack_from(endian+('Q' if wide else 'I'),data,32 if wide else 28)[0]
            size,count=struct.unpack_from(endian+'HH',data,54 if wide else 42)
            alignments=[]
            for i in range(count):
                at=offset+i*size
                if struct.unpack_from(endian+'I',data,at)[0]==1:
                    alignment=struct.unpack_from(endian+('Q' if wide else 'I'),data,at+(48 if wide else 28))[0]
                    if alignment<required_alignment or (alignment & (alignment - 1)) != 0:
                        raise ValueError(f'Native LOAD segment lacks {required_alignment // 1024} KB alignment: {name}')
                    alignments.append(alignment)
            if not alignments:
                raise ValueError(f'No native LOAD segments: {name}')
            libraries.append({'path':name,'load_alignment':min(alignments)})
    return libraries


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('artifacts',nargs='+',type=Path)
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--signed-release',action='store_true')
    parser.add_argument('--source-commit')
    args=parser.parse_args()
    records=[]
    for path in args.artifacts:
        native=verify_native(path)
        with zipfile.ZipFile(path) as z:
            signature=any(n.startswith('META-INF/') and n.upper().endswith(('.RSA','.DSA','.EC')) for n in z.namelist())
        if args.signed_release and path.suffix=='.aab' and not signature:
            raise ValueError('Release AAB has no signature block')
        records.append({'file':path.name,'bytes':path.stat().st_size,'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'jar_signature_block':signature,'native_libraries':native})
    commit=args.source_commit or subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()
    if not re.fullmatch(r'[0-9a-f]{40}',commit):
        raise ValueError('Expected an immutable Git commit SHA')
    source=Path('app/build.gradle.kts').read_text()
    version={'name':re.search(r'versionName\s*=\s*"([^"]+)"',source).group(1),
             'code':int(re.search(r'versionCode\s*=\s*(\d+)',source).group(1)),
             'target_sdk':int(re.search(r'targetSdk\s*=\s*(\d+)',source).group(1))}
    args.output.parent.mkdir(parents=True,exist_ok=True)
    args.output.write_text(json.dumps({'commit':commit,'version':version,'artifacts':records,'physical_device_acceptance':'NOT VERIFIED','play_submission':'NOT AUTHORIZED'},indent=2)+'\n')
    args.output.with_suffix('.sha256').write_text(''.join(f"{r['sha256']}  {r['file']}\n" for r in records))
    print(f'Verified {len(records)} archive(s); metadata: {args.output}')


if __name__=='__main__':
    main()
