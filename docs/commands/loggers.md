## 记录器

### 区块MSPT占用(chunkmspt)

`/log chunkmspt <option>`

一个用于显示当前区块具体MSPT占用的 HUD 记录器。它显示的信息如下：
- 区块坐标
- 区块占用总MSPT大小
- 区块实体总数
- 实体运算占用MSPT，用符号EU表示
- 计划刻运算占用MSPT，用符号TT表示
- 区块刻运算占用MSPT，用符号CT表示
- 方块更新运算占用MSPT，用符号BU表示
- 生物生成运算占用MSPT，用符号MS表示
- 方块事件运算占用MSPT，用符号BE表示
- 方块事件运算占用MSPT，用符号TE表示

![chunkmspt](https://picui.ogmua.cn/s1/2026/08/29/6a92f1fbc1ba3.webp)

可用的类型选项:

- `status`:输出MSPT占用前十的区块。

![chunkmsptstatus](https://picui.ogmua.cn/s1/2026/08/29/6a92f1fa61623.webp)