import ast
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import jitpack_build as build

class JitpackBuildTests(unittest.TestCase):
    def test_entrypoint_parses_on_jitpack_python_35(self):
        # The default JitPack image may provide Python older than f-strings.
        tree = ast.parse(Path(build.__file__).read_text(), feature_version=(3, 5))
        self.assertFalse(any(isinstance(node, ast.JoinedStr) for node in ast.walk(tree)))

    def test_git_output_uses_python_35_subprocess_options(self):
        def legacy_check_output(args, cwd, universal_newlines=False):
            self.assertEqual(args, ('git', 'rev-parse', 'HEAD'))
            self.assertEqual(cwd, str(build.ROOT))
            self.assertTrue(universal_newlines)
            return 'a' * 40 + '\n'

        with patch.object(build.subprocess, 'check_output', side_effect=legacy_check_output):
            self.assertEqual(build.run('git', 'rev-parse', 'HEAD'), 'a' * 40)

    def test_selection(self):
        for separator in ('/', '~'):
            self.assertEqual(build.selection(f'tfmodel{separator}v1.2.3'), ('tfmodel', '1.2.3'))
        for value in ('master', 'v1.2.3', 'app/v1.2.3', 'tfmodel/v01.2.3', 'tfmodel/v1.2.3-maven-central', 'tfmodel/v1.2.3-SNAPSHOT', 'tfmodel/v1.2.3/x'):
            with self.assertRaises(ValueError):
                build.selection(value)
        self.assertEqual(build.command('tfmodel', '1.2.3')[2], ':tfmodel:publishJitPack')
        self.assertNotIn(':carddetector:', ' '.join(build.command('tfmodel', '1.2.3')))

    def test_core_entrypoint_never_pulls_lfs_or_builds_sibling(self):
        with patch.dict(build.os.environ, {'VERSION': 'carddetector~v1.2.3', 'GIT_COMMIT': 'a'*40}), patch.object(build, 'run', return_value='a'*40), patch.object(build.subprocess, 'run') as execute:
            build.main()
        execute.assert_called_once_with(build.command('carddetector', '1.2.3'), cwd=str(build.ROOT), check=True)

    def test_scoped_tag_build_uses_exact_tag_and_scoped_coordinate(self):
        with patch.dict(build.os.environ, {'VERSION': 'carddetector~v1.2.3-jitpack', 'GIT_COMMIT': 'a'*40}), patch.object(build, 'run', return_value='a'*40) as git, patch.object(build.subprocess, 'run') as execute:
            build.main()
        git.assert_any_call('git', 'rev-parse', 'refs/tags/carddetector/v1.2.3-jitpack^{commit}')
        self.assertIn('-PjitpackScopedTag=true', execute.call_args.args[0])

    def test_model_entrypoint_scopes_lfs_before_build(self):
        with patch.dict(build.os.environ, {'VERSION': 'tfmodel~v1.2.3', 'GIT_COMMIT': 'a'*40}), patch.object(build, 'run', return_value='a'*40), patch.object(build.subprocess, 'run') as execute, patch.object(build, 'verify_models') as validate:
            build.main()
        self.assertEqual(execute.call_args_list[0].args[0], ['git', 'lfs', 'pull', '--include=' + build.MODEL_GLOB, '--exclude='])
        self.assertEqual(execute.call_args_list[1].args[0], build.command('tfmodel', '1.2.3'))
        validate.assert_called_once()

    def test_lfs_pointer_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory)/'model.tflite'
            path.write_text('version https://git-lfs.github.com/spec/v1\n')
            with patch.object(build,'ROOT',Path(directory)), patch.object(build,'run',return_value='model.tflite'):
                with self.assertRaisesRegex(ValueError,'LFS pointer'):
                    build.verify_models()
