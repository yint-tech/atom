import React, {createContext, useEffect, useState} from "react";
import moment from 'moment';
import apis from 'apis';
import {useSnackbar} from "notistack";
import {Loading} from "../components";

export const AppContext = createContext({});

const Adapter = (props) => {
    const {enqueueSnackbar} = useSnackbar();
    const [user, setUser] = useState({});
    const [api, setApi] = useState({});
    const [notice, setNotice] = useState('');
    const [firstLogin, setFirstLogin] = useState(false);

    useEffect(() => {
        let newApi = {};

        for (let i of Object.keys(apis)) {
            if (typeof apis[i] !== "function") {
                continue;
            }
            newApi[i] = function () {
                const args = [].slice.call(arguments)
                const that = this;
                return new Promise((resolve, reject) => {
                    apis[i].apply(that, args).then((res) => {
                        if (res.status !== 0) {
                            console.log("call api " + i + " error :" + res.message)
                            enqueueSnackbar(res.message.substring(0, 50), {
                                variant: "error",
                                anchorOrigin: {
                                    vertical: "top",
                                    horizontal: "center"
                                }
                            });
                        }
                        resolve(res);
                    }).catch(err => {
                        console.error(err);
                        reject(err);
                    })
                });
            }
        }
        newApi["urls"] = apis["urls"];
        newApi['getStore'] = apis['getStore'];
        newApi['setStore'] = apis['setStore'];
        newApi['errorToast'] = (msg) => {
            enqueueSnackbar(msg, {
                variant: "error",
                anchorOrigin: {
                    vertical: "top",
                    horizontal: "center"
                }
            });
        }
        newApi['successToast'] = (msg) => {
            enqueueSnackbar(msg, {
                variant: "success",
                anchorOrigin: {
                    vertical: "top",
                    horizontal: "center"
                }
            });
        }
        setApi(newApi)
    }, [enqueueSnackbar])

    useEffect(() => {
        let u = apis.getStore();
        setUser({
            ...u,
            time: moment(new Date()).format('YYYY-MM-DD HH:mm:ss')
        });
        const refreshUserInfo = () => {
            apis.notice().then(res => {
                if (res.status === 0) {
                    setNotice(res.data);
                }
            }).catch(() => {
            });
            return apis.getUser().then(res => {
                if (res.status === 0) {
                    u = {
                        ...apis.getStore(),
                        ...res.data,
                        time: moment(new Date()).format('YYYY-MM-DD HH:mm:ss')
                    };
                    apis.setStore(u);
                    setUser(u);
                }
            });
        }
        refreshUserInfo().then((res) => {
            setFirstLogin(true);
        });
        let timer = setInterval(() => {
            refreshUserInfo();
        }, 60 * 1000);
        return () => {
            timer && clearInterval(timer);
        }
    }, []);

    return (
        <AppContext.Provider
            value={{user, api, setUser, notice}}
        >
            {firstLogin ? props.children : <Loading/>}
        </AppContext.Provider>
    );
};

export default Adapter;
