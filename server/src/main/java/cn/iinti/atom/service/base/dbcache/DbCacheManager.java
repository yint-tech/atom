package cn.iinti.atom.service.base.dbcache;

import cn.iinti.atom.entity.UserInfo;
import cn.iinti.atom.mapper.UserInfoMapper;
import cn.iinti.atom.service.base.BroadcastService;
import cn.iinti.atom.service.base.dbcache.exs.UserEx;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Getter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


@Service
public class DbCacheManager {


    @Resource
    private UserInfoMapper userInfoMapper;

    @Getter
    private DbCacheStorage<UserInfo, UserEx> userCacheWithName;
    @Getter
    private DbCacheStorage<UserInfo, Void> userCacheWithId;
    @Getter
    private DbCacheStorage<UserInfo, Void> userCacheWithApiToken;

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    private void updateAllDbData() {
        BroadcastService.post(() -> {
            userCacheWithName.updateAll();
            userCacheWithApiToken.updateAll();
            userCacheWithId.updateAll();
        });
    }


    @PostConstruct
    public void init() {
        userCacheWithName = new DbCacheStorage<>(UserInfo.USER_NAME, userInfoMapper, updateHandlerUser);
        userCacheWithApiToken = new DbCacheStorage<>(UserInfo.API_TOKEN, userInfoMapper);
        userCacheWithId = new DbCacheStorage<>(UserInfo.ID, userInfoMapper);
        BroadcastService.register(BroadcastService.Topic.USER, () -> {
            userCacheWithName.updateAll();
            userCacheWithApiToken.updateAll();
            userCacheWithId.updateAll();
        });
    }

    private final DbCacheStorage.UpdateHandler<UserInfo, UserEx> updateHandlerUser = (userInfo, userEx) -> {
        if (userEx == null) {
            userEx = new UserEx();
        }
        userEx.reload(userInfo);
        return userEx;
    };
}
