import { useTheme } from "react-jss";
import {
    Button,
    Card,
    CardContent,
    CardHeader,
    Divider,
    Grid,
    MenuItem,
    Select,
    TextField,
    Typography
} from "@mui/material";
import { createUseStyles } from 'react-jss'; // 导入 createUseStyles
import React from "react";

// 使用 JSS 样式
const useStyles = createUseStyles({
    inputItem: {
        width: '100%',
        minHeight: '40px',
        '& .MuiInputBase-root': {
            height: '40px',  // 设置统一高度
        },
    },
    miniMargin: {
        margin: '32px 0', // 原来的间距
    },
    // 新增的 Divider 顶部间距样式
    dividerTopMargin: {
        marginTop: '16px', // 你可以根据需要调整顶部间距的值
        marginBottom: '16px', // 你可以根据需要调整底部间距的值
    },
});

// 统一的 onChange 处理函数
const handleChange = (fieldName, setSearchParam, searchParam, setRefresh) => (e) => {
    setSearchParam({ ...searchParam, [fieldName]: e.target.value, page: 1 });
    setRefresh(+new Date());
};

const SearchPanel = (props) => {
    const { searchParam, setSearchParam, setRefresh, fields, title } = props; // 从 props 获取 fields 配置和 title
    const theme = useTheme();
    const classes = useStyles({ theme });

    // 渲染每个配置项
    const renderField = (fieldItem) => {
        const { componentType, label, fieldName, placeholder, xs, options, onClick } = fieldItem;
        const onChange = handleChange(fieldName, setSearchParam, searchParam, setRefresh);
        //console.log('onClick:', onClick);
        // 根据 type 选择对应的组件
        if (componentType === 'select') {
            return (
                <Grid item xs={xs} key={fieldName}>
                    <Typography gutterBottom variant="inherit">{label}</Typography>
                    <Select
                        className={classes.inputItem}
                        variant="outlined"
                        value={searchParam[fieldName]}
                        onChange={onChange}
                        inputProps={{ style: { height: '40px' } }}
                    >
                        {Object.keys(options).map(k => (
                            <MenuItem key={k} value={k}>
                                <pre>{options[k]}</pre>
                            </MenuItem>
                        ))}
                    </Select>
                </Grid>
            );
        } else if (componentType === 'date') {
            return (
                <Grid item xs={xs} key={fieldName}>
                    <Typography gutterBottom variant="inherit">{label}</Typography>
                    <TextField
                        className={classes.inputItem}
                        value={searchParam[fieldName]}
                        size="small"
                        onChange={onChange}
                        type="date"
                        placeholder={placeholder}
                        InputLabelProps={{ shrink: true }}
                    />
                </Grid>
            );
        } else if (componentType === 'button') {  // 如果是 button 类型
            return (
                <Grid item xs={xs} key={fieldName}>
                    <Button
                        color="primary"
                        variant="contained"
                        onClick={onClick}  // 使用配置中的 onClick
                    >
                        {label}
                    </Button>
                </Grid>
            );
        }

        // 默认处理 text 类型
        return (
            <Grid item xs={xs} key={fieldName}>
                <Typography gutterBottom variant="inherit">{label}</Typography>
                <TextField
                    className={classes.inputItem}
                    value={searchParam[fieldName]}
                    size="small"
                    onChange={onChange}
                    placeholder={placeholder}
                    type="text"
                />
            </Grid>
        );
    };

    return (
        <Card>
            <CardHeader
                title={title} // 使用传入的 title
                titleTypographyProps={{
                    variant: 'h6'
                }}
            />
            <CardContent>
                {fields.map((row, rowIndex) => (
                [
                    <Grid container spacing={4} wrap="wrap" key={rowIndex}> {/* 使用稍大的间距 */}
                    {row.map((field, fieldIndex) => (
                        <Grid item xs={12} sm={6} md={4} lg={3} key={fieldIndex} > {/* 控制字段占位 */}
                            {renderField(field)}
                        </Grid>
                    ))}
                   </Grid>,
                   rowIndex < fields.length - 1 
                   && <Divider key={`divider-${rowIndex}`} 
                   className={classes.dividerTopMargin} /> // 添加顶部间距
                ]
        ))}
            </CardContent>
        </Card>
    );
};

export default SearchPanel;
