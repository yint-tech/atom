import {navbar} from "vuepress-theme-hope";

export const zhNavbar = navbar([
    {
        text: "序言",
        icon: "fa6-solid:book",
        prefix: "/01_preamble/",
        children: [
            {
                text: "简介",
                link: "01_introduce.md",
            },
            {
                text: "技术选型",
                link: "02_lectotype.md",
            }, {
                text: "安装",
                link: "代码范式.md",
            }
        ]
    },
    {
        text: "开发者",
        icon: "fa6-solid:laptop-code",
        prefix: "/02_developer/",
        children: [
            {
                text: "开始",
                link: "01_startup/",
            }
        ]
    }
]);
