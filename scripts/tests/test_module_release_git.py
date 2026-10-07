"""Real local Git remotes verify independent journals and atomic docs publication."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import urllib.error
from test_module_release import m

class IndependentGitTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup)
        self.base=Path(self.temp.name);self.remote=self.base/'remote.git';self.repo=self.base/'repo'
        self.call(self.base,'init','--bare','--initial-branch=main',str(self.remote))
        self.call(self.base,'clone',str(self.remote),str(self.repo))
        self.git('config','user.name','Test');self.git('config','user.email','test@example.invalid')
        (self.repo/'gradle.properties').write_text('GROUP=com.apexfission.android.carddetector\nPOM_ARTIFACT_ID=core\nMODEL_ARTIFACT_ID=sentinel-card-model\nmodelCoreVersion=0.1.0\n')
        self.git('add','.');self.git('commit','-m','initial source');self.initial=self.git('rev-parse','HEAD')
        self.git('tag','-a','v0.1.0',self.initial,'-m','legacy paired release')
        (self.repo/'docs/releases').mkdir(parents=True);(self.repo/'docs/templates').mkdir()
        (self.repo/'docs/templates/MODULE_IMPORT.md.template').write_text('# Install\n{{INSTALLATION}}\n')
        for module in m.MODULES:
            r=self.record(module,'0.1.0',self.initial);r.update(phase='confirmed-public',legacy_tag='v0.1.0')
            (self.repo/m.metadata_path(module)).write_text(json.dumps(r))
        self.patcher=patch.object(m,'ROOT',self.repo);self.patcher.start();self.addCleanup(self.patcher.stop)
        m.documentation();self.git('add','.');self.git('commit','-m','seed independent metadata');self.source=self.git('rev-parse','HEAD')
        self.git('push','origin','main','--tags')

    def call(self,cwd,*args):return subprocess.check_output(['git',*args],cwd=cwd,text=True,stderr=subprocess.PIPE).strip()
    def git(self,*args):return self.call(self.repo,*args)
    def remote_git(self,*args):return self.call(self.remote,*args)
    def record(self,module,version,source):
        r=dict(module=module,group='com.apexfission.android.carddetector',artifact=m.MODULES[module],version=version,source=source,phase='reserved-before-upload',sha256={s:'b'*64 for s in m.common.SUFFIXES})
        if module=='tfmodel':r['core_version']='0.1.0'
        return r
    def journal(self,module='carddetector',version='0.1.1'):
        r=self.record(module,version,self.source)
        self.git('tag','-a',m.pending(module,version),self.source,'-m',json.dumps(r));self.git('push','origin','--tags')
        return r
    def confirmed(self,r):
        r=dict(r,phase='confirmed-public');(self.repo/'build').mkdir(exist_ok=True)
        (self.repo/f'build/confirmed-{r["module"]}.json').write_text(json.dumps(r))

    def test_prepare_queries_only_core_despite_pending_model(self):
        self.journal('tfmodel')
        with patch.object(m.common,'published_versions',return_value=['0.1.0']) as fetch:
            selected=m.prepare('carddetector')
        self.assertEqual(selected['version'],'0.1.1')
        fetch.assert_called_once_with('com.apexfission.android.carddetector','core')

    def test_model_allocation_can_follow_different_core_version_history(self):
        self.journal('carddetector','9.0.0')
        with patch.object(m.common,'published_versions',return_value=['0.1.0']),patch.object(m,'ensure_core_public'):
            selected=m.prepare('tfmodel')
        self.assertEqual(selected['version'],'0.1.1')

    def test_same_source_can_upload_each_module_but_never_twice(self):
        for module in m.MODULES:
            self.journal(module);m.guard(module,'0.1.1')
            self.assertEqual(self.remote_git('rev-parse',f'{m.uploading(module,"0.1.1")}^{{commit}}'),self.source)
            with self.assertRaisesRegex(ValueError,'already started'):m.guard(module,'0.1.1')

    def test_finalization_keeps_newer_sibling_metadata(self):
        r=self.journal();self.confirmed(r)
        sibling=self.record('tfmodel','0.1.1',self.source);sibling['phase']='confirmed-public'
        (self.repo/m.metadata_path('tfmodel')).write_text(json.dumps(sibling));m.documentation()
        self.git('add','docs','IMPORT.md');self.git('commit','-m','model finalized first');self.git('tag','-a',m.tag('tfmodel','0.1.1'),self.source,'-m','model');self.git('push','origin','main','--tags')
        self.git('checkout','--detach',self.source)
        m.finalize('carddetector','0.1.1',self.source)
        final=json.loads(self.remote_git('show','main:'+m.metadata_path('tfmodel')))
        self.assertEqual(final,sibling)
        self.assertEqual(self.remote_git('rev-parse',m.tag('carddetector','0.1.1')+'^{commit}'),self.source)
        self.assertIsNone(m.remote_ref(m.pending('carddetector','0.1.1')))
        result=m.prepare_finalization('carddetector','0.1.1',self.source)
        self.assertEqual(result['skip'],'true')

    def test_atomic_rejection_preserves_pending_and_main(self):
        r=self.journal();self.confirmed(r);before=self.remote_git('rev-parse','main')
        hook=self.remote/'hooks/pre-receive';hook.write_text('#!/bin/sh\nexit 1\n');hook.chmod(0o755)
        with self.assertRaises(subprocess.CalledProcessError):m.finalize('carddetector','0.1.1',self.source)
        self.assertEqual(self.remote_git('rev-parse','main'),before)
        self.assertIsNotNone(m.remote_ref(m.pending('carddetector','0.1.1')))
        self.assertIsNone(m.remote_ref(m.tag('carddetector','0.1.1')))

    def test_wrong_expected_source_recovery_stops(self):
        self.journal()
        with self.assertRaisesRegex(ValueError,'Conflicting source'):m.prepare_finalization('carddetector','0.1.1','f'*40)

    def test_confirmation_timeout_changes_no_committed_metadata(self):
        self.journal();before=(self.repo/'IMPORT.md').read_bytes()
        error=urllib.error.HTTPError('url',404,'not propagated',{},None)
        with patch.object(m,'verify_public',side_effect=error),self.assertRaises(TimeoutError):m.confirm('carddetector','0.1.1',0)
        self.assertEqual((self.repo/'IMPORT.md').read_bytes(),before)
        self.assertFalse((self.repo/'build/confirmed-carddetector.json').exists())
        self.assertIsNotNone(m.remote_ref(m.pending('carddetector','0.1.1')))

    def test_model_dependency_pin_cannot_change_after_reservation(self):
        self.journal('tfmodel')
        p=self.repo/'gradle.properties';p.write_text(p.read_text().replace('modelCoreVersion=0.1.0','modelCoreVersion=0.1.1'))
        with self.assertRaisesRegex(ValueError,'Pinned core differs'):m.guard('tfmodel','0.1.1')

    def test_rename_continues_module_version_without_public_new_artifact(self):
        p=self.repo/'gradle.properties'
        p.write_text(p.read_text().replace('POM_ARTIFACT_ID=core','POM_ARTIFACT_ID=card-detector'))
        self.git('add','.');self.git('commit','-m','rename');self.git('push','origin','main')
        with patch.object(m.common,'published_versions',return_value=[]) as fetch:
            result=m.prepare('carddetector')
        self.assertEqual(result['version'],'0.1.1')
        fetch.assert_called_once_with('com.apexfission.android.carddetector','card-detector')

    def test_self_hosted_finalize_preserves_central_and_has_independent_upload_guard(self):
        central=(self.repo/'docs/releases/carddetector.json').read_bytes()
        env={'RELEASE_REPOSITORY':'apexfission-maven','MAVEN_REPOSITORY_URL':'https://maven.example/releases'}
        folder=self.repo/'docs/releases/apexfission-maven';folder.mkdir()
        for module in m.MODULES:(folder/f'{module}.json').write_text('null\n')
        self.git('add','.');self.git('commit','-m','enable alternative repository')
        self.source=self.git('rev-parse','HEAD');self.git('push','origin','main')
        self.journal('carddetector','0.1.1')  # Central remains pending throughout.
        with patch.dict(os.environ,env):
            with patch.object(m,'published_versions',return_value=[]):
                self.assertEqual(m.prepare('carddetector')['version'],'0.1.0')
            r=self.record('carddetector','0.1.0',self.source)
            r.update(repository='apexfission-maven',repository_url='https://maven.example/releases')
            self.git('tag','-a',m.pending('carddetector','0.1.0'),self.source,'-m',json.dumps(r));self.git('push','origin','--tags')
            m.guard('carddetector','0.1.0')
            with self.assertRaisesRegex(ValueError,'already started'):m.guard('carddetector','0.1.0')
            with patch.object(m,'verify_public'):m.confirm('carddetector','0.1.0',0)
            m.finalize('carddetector','0.1.0',self.source)
            self.assertEqual(m.prepare_finalization('carddetector','0.1.0')['skip'],'true')
        self.assertEqual((self.repo/'docs/releases/carddetector.json').read_bytes(),central)
        self.assertIsNotNone(m.remote_ref('release-pending/carddetector/0.1.1'))
        self.assertIsNone(m.remote_ref('release-pending/apexfission-maven/carddetector/0.1.0'))
        self.assertEqual(self.remote_git('rev-parse','apexfission-maven/carddetector/v0.1.0^{commit}'),self.source)
        self.assertIn('https://maven.example/releases',(self.repo/'IMPORT.md').read_text())
        m.documentation(verify=True)  # Generating docs needs no environment credentials.

    def test_new_configured_target_finalizes_without_seed_metadata(self):
        entries={'maven-central':{'publisher':'central','environment':'maven-central'},'third-party':{'publisher':'maven','environment':'third-party-production'}}
        with patch.object(m,'load_config',return_value=entries),patch.dict(os.environ,{'RELEASE_REPOSITORY':'third-party','MAVEN_REPOSITORY_URL':'https://third.example/releases'}):
            with patch.object(m,'published_versions',return_value=[]):
                self.assertEqual(m.prepare('carddetector')['version'],'0.1.0')
            r=self.record('carddetector','0.1.0',self.source)
            r.update(repository='third-party',repository_url='https://third.example/releases')
            self.git('tag','-a',m.pending('carddetector','0.1.0'),self.source,'-m',json.dumps(r));self.git('push','origin','--tags')
            self.assertEqual(m.prepare_finalization('carddetector','0.1.0')['skip'],'false')
            m.guard('carddetector','0.1.0')
            with patch.object(m,'verify_public'):m.confirm('carddetector','0.1.0',0)
            m.finalize('carddetector','0.1.0',self.source)
            self.assertEqual(m.prepare_finalization('carddetector','0.1.0')['skip'],'true')
            self.assertEqual(json.loads((self.repo/'docs/releases/third-party/carddetector.json').read_text())['repository'],'third-party')
        m.documentation(verify=True)
        self.assertIn('https://third.example/releases',(self.repo/'IMPORT.md').read_text())
