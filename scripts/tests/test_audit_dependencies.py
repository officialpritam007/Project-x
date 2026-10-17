import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('dependency_audit', Path(__file__).resolve().parents[1] / 'audit_dependencies.py')
audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)


class DependencyAuditTest(unittest.TestCase):
    def test_deno_peer_context_does_not_change_package_or_version(self):
        self.assertEqual(audit.parse_npm_coordinate('isows@1.0.7_ws@8.22.0'), ('isows', '1.0.7'))
        self.assertEqual(audit.parse_npm_coordinate('@scope/pkg@1.2.3_peer@4.5.6'), ('@scope/pkg', '1.2.3'))

    def test_all_advisory_pages_are_merged_for_the_correct_package(self):
        records = [{'ecosystem': 'npm', 'name': 'fixture', 'version': '1.0.0'},
                   {'ecosystem': 'Maven', 'name': 'fixture:other', 'version': '2.0.0'}]
        pages = [{'results': [{'vulns': [{'id': 'GHSA-first'}], 'next_page_token': 'page2'}, {}]},
                 {'results': [{'vulns': [{'id': 'GHSA-second'}]}]}]
        with patch.object(audit, 'get_json', side_effect=pages) as fetch:
            audit.query_osv(records)
        self.assertEqual(records[0]['osv_advisories'], ['GHSA-first', 'GHSA-second'])
        self.assertEqual(records[1]['osv_advisories'], [])
        self.assertEqual(fetch.call_args.args[1]['queries'][0]['page_token'], 'page2')
        self.assertEqual(fetch.call_args.args[1]['queries'][0]['package']['name'], 'fixture')

    def test_truncated_response_cannot_be_treated_as_clean(self):
        with patch.object(audit, 'get_json', return_value={'results': []}):
            with self.assertRaises(ValueError):
                audit.query_osv([{'ecosystem': 'npm', 'name': 'fixture', 'version': '1.0.0'}])

    def test_repeated_pagination_is_an_error(self):
        with patch.object(audit, 'get_json', return_value={'results': [{'next_page_token': 'same'}]}):
            with self.assertRaises(ValueError):
                audit.query_osv([{'ecosystem': 'npm', 'name': 'fixture', 'version': '1.0.0'}])
