import React, {useContext, useEffect, useState} from "react";
import {makeStyles} from "@material-ui/styles";
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Button,
    Card,
    Divider,
    Grid,
    MenuItem,
    Select,
    Switch,
    TextField,
    Typography
} from "@material-ui/core";

import ExpandMoreIcon from "@material-ui/icons/ExpandMore";

import {Loading} from "components";
import {AppContext} from "adapter";

const useStyles = makeStyles(theme => ({
    root: {
        margin: theme.spacing(4)
    },
    heading: {
        fontSize: theme.typography.pxToRem(15),
        flexBasis: "33.33%",
        flexShrink: 0
    },
    secondaryHeading: {
        fontSize: theme.typography.pxToRem(15),
        color: theme.palette.text.secondary,
        maxWidth: "300px",
        overflow: "hidden",
        whiteSpace: "nowrap",
        textOverflow: "ellipsis"
    },
    desc: {
        fontSize: theme.typography.pxToRem(15),
        color: theme.palette.text.secondary
    },
    input: {
        display: "flex",
        alignItems: "center"
    },
    inputItem: {
        width: "100%"
    },
    inputBtn: {
        marginLeft: theme.spacing(2)
    },
    gutterTop: {
        marginTop: theme.spacing(2)
    },
    divider: {
        marginTop: theme.spacing(1),
        marginBottom: theme.spacing(2)
    },
    actions: {
        justifyContent: "center"
    },
    noMaxWidth: {
        maxWidth: "none"
    }
}));

function SingleInputItem({
                             placeholder = "",
                             initValue = "",
                             initKey = "",
                             type = "input",
                             options = [],
                             multiline = false,
                             reload = () => {
                             }
                         }) {
    const classes = useStyles();
    const {api} = useContext(AppContext);
    const [value, setValue] = useState("");
    useEffect(() => {
        setValue(initValue);
    }, [initValue]);

    const doSave = () => {
        api.setConfig({key: initKey, value}).then(res => {
            if (res.status === 0) {
                api.successToast("修改成功");
            }
            reload();
        })
    };

    return (
        <Grid item xs={12} className={classes.input}>
            {
                type === "input" ? (
                    <TextField
                        className={classes.inputItem}
                        multiline={multiline}
                        rows={multiline ? 4 : undefined}
                        size="small"
                        variant="outlined"
                        placeholder={placeholder}
                        value={value}
                        onChange={(e) => setValue(e.target.value)}/>
                ) : null
            }
            {
                type === "switch" ? (
                    <Switch
                        checked={value || false}
                        onChange={(e) => setValue(e.target.checked)}
                        inputProps={{"aria-label": "secondary checkbox"}}
                    />
                ) : null
            }
            {
                type === "select" ? (
                    <Select
                        className={classes.inputItem}
                        variant="outlined"
                        value={value}
                        onChange={(e) => setValue(e.target.value)}
                    >
                        {options.map(item => (
                            <MenuItem key={item} value={item}>
                                {item}
                            </MenuItem>
                        ))}
                    </Select>
                ) : null
            }
            <Button className={classes.inputBtn} variant="contained" color="primary" onClick={doSave}>保存</Button>
        </Grid>
    );
}

const Form = () => {
    const classes = useStyles();
    const [configs, setConfigs] = useState([]);

    const [refresh, setRefresh] = useState(+new Date());
    const {api} = useContext(AppContext);
    useEffect(() => {
        api.settingTemplate().then(res => {
            if (res.status === 0) {
                setConfigs(res.data.normal);
            }
        })
    }, [api, refresh]);

    return (
        <Card className={classes.root}>
            {configs.length > 0 ? <>
                    {configs.map((item, index) => (
                        <Accordion key={"panel" + index}>
                            <AccordionSummary expandIcon={<ExpandMoreIcon/>}>
                                <Typography className={classes.heading}>{item.desc}</Typography>
                                <Typography className={classes.secondaryHeading}>{item.key}</Typography>
                            </AccordionSummary>
                            <AccordionDetails>
                                <div style={{width: "100%"}}>
                                    <Typography className={classes.desc}>填写说明：{item.detailDesc}</Typography>
                                    <Divider className={classes.divider}/>
                                    <Grid container spacing={6} wrap="wrap">
                                        <SingleInputItem
                                            type={item.type === 'Boolean' ? "switch" : "input"}
                                            placeholder={item.desc}
                                            initKey={item.key}
                                            initValue={item.value}
                                            reload={() => setRefresh(+new Date())}/>
                                    </Grid>
                                </div>
                            </AccordionDetails>
                        </Accordion>
                    ))}
                </> :
                <Loading/>
            }
        </Card>
    );
};

export default Form;
