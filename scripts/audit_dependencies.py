#!/usr/bin/env python3
"""Read-only OSV snapshot plus npm license/integrity metadata. No packages execute.

Exit0: complete catalog queries/no matches. Exit1: advisories need triage.
Exit2: incomplete/error/integrity mismatch. None of these means legal clearance
or certification that the app has no vulnerabilities.
"""
import argparse
import concurrent.futures
import datetime
import hashlib
import json
from pathlib import Path
import urllib.parse
import urllib.request


def get_json(url, payload=None):
    request = urllib.request.Request(url, data=None if payload is None else json.dumps(payload).encode(),
                                     headers={"Content-Type": "application/json", "User-Agent": "LiquidChat-dependency-audit/1"})
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


def npm_metadata(record):
    try:
        meta = get_json("https://registry.npmjs.org/" + urllib.parse.quote(record['name'], safe='') + '/' + record['version'])
        return {**record, "licenses": meta.get('license'),
                "registry_integrity_matches_lock": meta.get('dist', {}).get('integrity') == record['lock_integrity']}
    except Exception as error:
        return {**record, "metadata_error": type(error).__name__}


def parse_npm_coordinate(coordinate):
    # Deno v5 may append peer contexts: isows@1.0.7_ws@8.22.0.
    separator = coordinate.find('@', 1)
    if separator < 1:
        raise ValueError('Malformed npm lock coordinate')
    name, version = coordinate[:separator], coordinate[separator + 1:].split('_', 1)[0]
    if not version:
        raise ValueError('Missing npm lock version')
    return name, version


def query_osv(records):
    pending = [(index, None) for index in range(len(records))]
    seen = set()
    pages = 0
    while pending:
        queries = []
        for index, token in pending:
            record = records[index]
            query = {"package": {"ecosystem": record['ecosystem'], "name": record['name']}, "version": record['version']}
            if token:
                query['page_token'] = token
            queries.append(query)
        result = get_json('https://api.osv.dev/v1/querybatch', {"queries": queries})
        if len(result.get('results', [])) != len(queries):
            raise ValueError('OSV result count differs from query count')
        remaining = []
        for (index, _), answer in zip(pending, result['results']):
            ids = records[index].setdefault('osv_advisories', [])
            ids[:] = sorted(set(ids) | {item['id'] for item in answer.get('vulns', [])})
            token = answer.get('next_page_token')
            if token:
                if (index, token) in seen:
                    raise ValueError('OSV pagination repeated')
                seen.add((index, token))
                remaining.append((index, token))
        pages += 1
        if pages > 100:
            raise ValueError('OSV pagination incomplete after100 pages')
        pending = remaining


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--lock', type=Path, required=True)
    parser.add_argument('--inventory', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    sources = {str(args.lock): hashlib.sha256(args.lock.read_bytes()).hexdigest()}
    lock = json.loads(args.lock.read_text())
    npm = []
    for coordinate, value in sorted(lock['npm'].items()):
        name, version = parse_npm_coordinate(coordinate)
        npm.append({"ecosystem": "npm", "name": name, "version": version, "resolved_coordinate": coordinate, "lock_integrity": value['integrity']})
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        records = list(pool.map(npm_metadata, npm))
    if args.inventory:
        sources[str(args.inventory)] = hashlib.sha256(args.inventory.read_bytes()).hexdigest()
        inventory = json.loads(args.inventory.read_text())
        if not inventory.get('packages'):
            raise ValueError('Resolved Android inventory is empty')
        records.extend(inventory['packages'])
    if not records:
        raise ValueError('No dependency records')
    report = {"checked_at": datetime.datetime.now(datetime.timezone.utc).isoformat(),
              "source_sha256": sources, "android_inventory_included": bool(args.inventory),
              "scope": "Selected Android release Maven modules when supplied, plus locked npm graph. Excludes build plugins, OS/native vulnerabilities and legal clearance.",
              "osv_status": "UNKNOWN", "packages": records}
    code = 2
    try:
        query_osv(records)
        report['osv_status'] = 'COMPLETE'
        matches = sum(bool(record.get('osv_advisories')) for record in records)
        incomplete = any('metadata_error' in r or r.get('registry_integrity_matches_lock') is False for r in records)
        code = 2 if incomplete else (1 if matches else 0)
        report['matched_packages'] = matches
        report['npm_metadata_incomplete_or_mismatched'] = incomplete
    except Exception as error:
        report['query_error'] = type(error).__name__
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + '\n')
    print(f"Audited {len(records)} records; OSV {report['osv_status']}; exit{code}. License gaps require review.")
    return code


if __name__ == '__main__':
    raise SystemExit(main())
