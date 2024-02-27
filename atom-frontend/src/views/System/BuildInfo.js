import React, {useContext, useEffect, useState} from "react";
import {AppContext} from "adapter";
import {Card, CardContent, Divider, Typography} from "@material-ui/core";


const BuildInfo = (pros) => {
    const [data, setData] = useState({});
    const {api} = useContext(AppContext);

    useEffect(() => {
        api.systemInfo().then(res => {
            if (res.status === 0) {
                setData(res.data)
            }
        });
    }, [api])

    return (<Card>

        <CardContent>
            <Typography gutterBottom variant="h4">
                构建时间
            </Typography>
            <Typography gutterBottom variant="subtitle2">
                {data.buildTime}
            </Typography>
            <Divider/>
            <Typography gutterBottom variant="h4">
                编译主机
            </Typography>
            <Typography gutterBottom variant="subtitle2">
                {data.buildUser}
            </Typography>
            <Divider/>
            <Typography gutterBottom variant="h4">
                gitId
            </Typography>
            <Typography gutterBottom variant="subtitle2">
                {data.gitId}
            </Typography>

            <Divider/>
            <Typography gutterBottom variant="h4">
                版本
            </Typography>
            <Typography gutterBottom variant="subtitle2">
                {data.versionName}
            </Typography>

            <Divider/>
            <Typography gutterBottom variant="h4">
                版本号
            </Typography>
            <Typography gutterBottom variant="subtitle2">
                {data.versionCode}
            </Typography>
        </CardContent>
    </Card>);
}

export default BuildInfo;