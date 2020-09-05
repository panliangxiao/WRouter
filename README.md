# WRouter

## android路由框架

特点：
- 相比于ARouter支持多路径


## 支持多路径

### 多路径使用方式

```java

@Route(paths = "/main/hello")
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

}
@Route(paths = {"/share/1", "/share/3333"})
public class ShareActivity extends Activity{
}
```

