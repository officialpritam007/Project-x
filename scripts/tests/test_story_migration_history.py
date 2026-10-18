"""Guard the deployed Supabase video-story migration version against replays."""
from pathlib import Path
import unittest

MIGRATIONS = Path(__file__).resolve().parents[2] / "supabase" / "migrations"


class StoryMigrationHistoryTest(unittest.TestCase):
    def test_local_version_matches_deployed_project(self):
        # Deployed project qsdmspylumxysczzqnqc reports 20261018161233.
        expected = MIGRATIONS / "20261018161233_secure_30_second_video_stories.sql"
        self.assertTrue(expected.is_file())
        self.assertFalse((MIGRATIONS / "20261018161000_secure_30_second_video_stories.sql").exists())

    def test_video_story_migration_is_registered_once(self):
        files = list(MIGRATIONS.glob("*secure_30_second_video_stories.sql"))
        self.assertEqual([p.name for p in files],
                         ["20261018161233_secure_30_second_video_stories.sql"])


if __name__ == "__main__":
    unittest.main()
