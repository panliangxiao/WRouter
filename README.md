# WRouter

特点：
- 支持多路径
- 优化路由启动时间

## 支持多路径

### 多路径

```java

@Route(paths = "/main/hello")
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

}
@Route(paths = {"/share/1", "/share/3333"})
public class ShareActivity extends Activity{
}
```

## 优化路由启动时间

### 路由字节码插桩方式优化
```java
    private static void loadRouterMap() {
        registerByPlugin = false;
        //auto generate register code by gradle plugin: wrouter-auto-register
        // looks like below:
        // registerRouteRoot(new WRouter__Root__modulejava());
        // registerRouteRoot(new WRouter__Root__modulekotlin());
    }

    /**
     * register by object
     * Sacrificing a bit of efficiency to solve
     * the problem that the main dex file size is too large
     * @param obj class name
     */
    private static void register(Object obj) {
            try {
                if (obj instanceof IRouteRoot) {
                    registerRouteRoot((IRouteRoot) obj);
                } else if (obj instanceof IProviderGroup) {
                    registerProvider((IProviderGroup) obj);
                } else if (obj instanceof IInterceptorGroup) {
                    registerInterceptor((IInterceptorGroup) obj);
                } else {
                    logger.info(TAG, "register failed, class name: " + obj.toString()
                            + " should implements one of IRouteRoot/IProviderGroup/IInterceptorGroup.");
                }
            } catch (Exception e) {
                logger.error(TAG,"register class error:" + (obj == null ? "": obj.toString()));
            }
    }
```
