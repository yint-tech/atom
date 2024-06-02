package cn.iinti.atom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户信息
 * </p>
 *
 * @author iinti
 * @since 2022-12-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_info")
@Schema(name = "UserInfo对象", description = "用户信息")
public class UserInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(name = "用户名")
    private String userName;

    @Schema(name = "密码")
    private String password;

    @Schema(name = "最后登陆时间")
    private LocalDateTime lastActive;

    @Schema(name = "创建时间")
    private LocalDateTime createTime;

    @Schema(name = "登录token")
    private String loginToken;

    @Schema(name = "api 访问token")
    private String apiToken;

    @Schema(name = "是否是管理员")
    private Boolean isAdmin;

    @Schema(name = "最后更新时间")
    private LocalDateTime updateTime;

    @Schema(name = "用户权限")
    private String permission;

    public static final String ID = "id";

    public static final String USER_NAME = "user_name";

    public static final String PASSWORD = "password";

    public static final String LAST_ACTIVE = "last_active";

    public static final String CREATE_TIME = "create_time";

    public static final String LOGIN_TOKEN = "login_token";

    public static final String API_TOKEN = "api_token";

    public static final String IS_ADMIN = "is_admin";

    public static final String UPDATE_TIME = "update_time";

    public static final String PERMISSION = "permission";

}
