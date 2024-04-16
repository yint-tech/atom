import {defineUserConfig} from 'vuepress'
import {head} from "./configs";
import theme from "./theme.js";
import viteBundler from "@vuepress/bundler-vite";

export default defineUserConfig({
    base: "/atom-doc/",
    head,
    lang: 'zh-CN',
    title: 'atom',
    description: 'java单体全栈开发脚手架',
    bundler: viteBundler(),
    theme
})