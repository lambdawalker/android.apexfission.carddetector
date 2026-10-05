import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';
export default defineConfig({
  site: 'https://lambdawalker.github.io', base: '/android.apexfission.carddetector',
  integrations: [starlight({
    title: 'Card Detector', description: 'Detect, track, and crop cards on Android.',
    components: { Banner: './src/components/VersionBanner.astro' },
    customCss: ['./src/styles/brand.css'],
    social: [{icon:'github',label:'GitHub',href:'https://github.com/lambdawalker/android.apexfission.carddetector'}],
    sidebar: [
      {label:'Overview',slug:'index'},
      {label:'Start',items:[{label:'Install',slug:'installation'},{label:'First detection',slug:'getting-started'},{label:'Model contract',slug:'model-contract'}]},
      {label:'Integrate',items:[{label:'Configure detection',slug:'configuration'},{label:'Threads & ownership',slug:'concepts'},{label:'Recipes',slug:'task-recipes'},{label:'Runnable demos',slug:'demos'}]},
      {label:'Reference',items:[{autogenerate:{directory:'api'}}]},
      {label:'API index',slug:'reference'},
      {label:'Support',items:[{label:'Limitations',slug:'limitations'},{label:'Troubleshooting',slug:'troubleshooting'},{label:'Migration',slug:'migration'},{label:'Build & maintain',slug:'development'},{label:'Release runbook',slug:'releases'},{label:'Feature coverage',slug:'coverage'},{label:'Media provenance',slug:'media'},{label:'AI documentation',slug:'agents'}]},
    ],
  })],
});
