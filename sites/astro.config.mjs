import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';
export default defineConfig({
  site: 'https://lambdawalker.github.io', base: '/android.apexfission.carddetector',
  integrations: [starlight({
    title: 'Card Detector', description: 'Detect, track, and crop cards on Android.',
    components: { Banner: './src/components/VersionBanner.astro', Sidebar: './src/components/VersionSidebar.astro', LanguageSelect: './src/components/EmptyLanguageSelect.astro', Head: './src/components/VersionHead.astro' },
    defaultLocale: 'en',
    locales: { en: { label: 'English', lang: 'en' }, es: { label: 'Español', lang: 'es' } },
    customCss: ['./src/styles/brand.css'],
    social: [{icon:'github',label:'GitHub',href:'https://github.com/lambdawalker/android.apexfission.carddetector'}],
    sidebar: [],
  })],
});
