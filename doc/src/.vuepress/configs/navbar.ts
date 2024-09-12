import {navbar} from "vuepress-theme-hope";

export const zhNavbar = navbar([
    {
        text: "开发者",
        icon: "fa6-solid:laptop-code",
        prefix: "/02_developer/",
        children: [
            {
                text: "开始",
                link: "01_startup.md",
            }, {
                text: "编码",
                link: "02_coding.md",
            }, {
                text: "构建",
                link: "03_build.md",
            }, {
                text: "项目转换",
                link: "04_project_transform.md",
            }
        ]
    },
    {
        text: "中间件",
        icon: "fa6-solid:laptop-code",
        prefix: "/03_middleware/",
        children: [
            {
                text: "配置中心",
                link: "01_config.md",
            },
            {
                text: "监控",
                link: "02_metric.md",
            },
        ]
    }
]);
