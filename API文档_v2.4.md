# GamingBar RESTful API 接口文档

**项目名称**: GamingBar - 大学生游戏约玩平台  
**基础路径**: `/api`  
**返回格式**: `application/json`  
**认证方式**: JWT Bearer Token  
**文档版本**: v2.4.1（最终联调上线版）  
**更新日期**: 2026-04-17  
**适用范围**: 前端联调、后端实现、数据库落表、上线前审核

---

## 0. 文档说明

### 0.1 目标

本文档只定义当前版本上线必需的 RESTful API、数据库表设计、状态规则和一致性规则，不包含性能优化方案和未来扩展设计。

### 0.2 处理分工

本次文档整理按两个子代理分工执行，但文档内容以接口定义为中心：

| 子代理 | 作用 | 输出 |
|------|------|------|
| 文档编写子代理 | 汇总接口、字段、表设计、事务与示例 | 最终接口草案 |
| 审核子代理 | 拦截联调阻塞项、一致性缺口、字段遗漏 | 审核意见并回写最终版 |

### 0.3 上线阻塞项定义

以下任一项未明确，视为阻塞上线：
- 统一返回结构不固定
- 鉴权要求不明确
- 请求或响应字段缺少类型、必填、可空说明
- 枚举值不固定
- 房间状态流转不固定
- 失效房间处理顺序不固定
- `t_room.current_player` 与 `t_room_user` 一致性无保障
- 接口与数据库表映射不清楚
- 写接口事务边界不清楚

---

## 一、通用规范

### 1.1 统一返回结构

所有接口统一返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

| 字段 | 类型 | 必填 | 可空 | 说明 |
|------|------|------|------|------|
| code | int | 是 | 否 | `0` 表示成功，非 `0` 表示失败 |
| message | string | 是 | 否 | 结果描述 |
| data | object/array/null | 是 | 是 | 业务数据；失败场景固定返回 `null` |

### 1.2 分页返回结构

分页接口固定返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 100,
    "page": 1,
    "size": 20,
    "list": []
  }
}
```

| 字段 | 类型 | 必填 | 可空 | 说明 |
|------|------|------|------|------|
| total | int | 是 | 否 | 总记录数 |
| page | int | 是 | 否 | 当前页码 |
| size | int | 是 | 否 | 当前页大小 |
| list | array | 是 | 否 | 列表数据，无数据时返回 `[]` |

规则：
- 分页接口不得返回裸数组
- `page` 超范围时，仍返回请求页码，`list=[]`
- 失败时 `data=null`

### 1.3 通用错误码

| code | message | 说明 |
|------|---------|------|
| 0 | success | 成功 |
| 400 | 参数不合法 | 请求参数格式错误、缺参、枚举非法 |
| 401 | 未登录或 token 无效 | 未携带 token、token 无效或过期 |
| 403 | 无权限 | 当前用户无权执行该操作 |
| 404 | 资源不存在或已失效 | 资源不存在、已关闭、已清理或已判定失效 |
| 409 | 业务冲突 | 业务状态冲突，如房间已满、已在其他房间 |
| 500 | 服务器内部错误 | 未处理异常 |

### 1.4 鉴权

除登录相关接口外，所有接口必须在请求头中携带：

```http
Authorization: Bearer <access_token>
```

### 1.5 Token 规则

- 仅提供 `access token`
- 不提供 `refresh token`
- 有效期 7 天，即 `604800` 秒
- 过期后必须重新验证码登录

### 1.6 请求格式

- `POST`、`PUT` 默认使用 `application/json`
- `GET`、`DELETE` 使用路径参数和 query 参数

### 1.7 命名与可空规则

- 接口字段统一使用 `snake_case`
- 数据库字段统一使用 `snake_case`
- 文档标注为 `string/null`、`object/null` 的字段，后端只能返回对应类型或 `null`
- 文档未标注可空的字段，不得返回 `null`
- 空列表一律返回 `[]`

### 1.8 时间规则

- 时间格式统一为 `yyyy-MM-dd HH:mm:ss`
- 时区统一为 `Asia/Shanghai (UTC+8)`
- `type=instant` 时，`start_time` 固定为 `null`

### 1.9 枚举字典

| 字段 | 枚举值 |
|------|--------|
| room.type | `instant`, `scheduled` |
| room.status | `waiting`, `ready`, `closed` |
| game.status | `enabled`, `disabled` |
| sms.used_status | `0`, `1` |
| leave.action | `left`, `room_closed` |

---

## 二、数据模型

### 2.1 User

| 字段 | 类型 | 必填 | 可空 | 说明 |
|------|------|------|------|------|
| id | long | 是 | 否 | 用户 ID |
| phone | string | 是 | 否 | 11 位手机号 |
| nickname | string | 是 | 否 | 昵称，2-20 字符 |
| avatar | string | 是 | 否 | 头像 URL，可为空字符串 |
| credit_score | int | 是 | 否 | 信用分，默认 `100` |

### 2.2 Game

| 字段 | 类型 | 必填 | 可空 | 说明 |
|------|------|------|------|------|
| id | int | 是 | 否 | 游戏 ID |
| game_name | string | 是 | 否 | 游戏名称 |
| status | string | 是 | 否 | `enabled` / `disabled` |

### 2.3 RoomListItem

| 字段 | 类型 | 必填 | 可空 | 说明 |
|------|------|------|------|------|
| id | long | 是 | 否 | 房间 ID |
| game_id | int | 是 | 否 | 游戏 ID |
| game_name | string | 是 | 否 | 来自 `t_game.game_name` |
| owner_id | long | 是 | 否 | 房主用户 ID |
| owner_nickname | string | 是 | 否 | 来自 `t_user.nickname` |
| max_player | int | 是 | 否 | 最大人数，2-10 |
| current_player | int | 是 | 否 | 当前人数 |
| type | string | 是 | 否 | `instant` / `scheduled` |
| start_time | string/null | 是 | 是 | 预约时间 |
| status | string | 是 | 否 | `waiting` / `ready` / `closed` |
| create_time | string | 是 | 否 | 创建时间 |
| is_joined | boolean | 是 | 否 | 当前用户是否已加入，衍生字段 |

### 2.4 RoomMyItem

| 字段 | 类型 | 必填 | 可空 | 说明 |
|------|------|------|------|------|
| id | long | 是 | 否 | 房间 ID |
| game_id | int | 是 | 否 | 游戏 ID |
| game_name | string | 是 | 否 | 游戏名称 |
| owner_id | long | 是 | 否 | 房主用户 ID |
| owner_nickname | string | 是 | 否 | 房主昵称 |
| max_player | int | 是 | 否 | 最大人数 |
| current_player | int | 是 | 否 | 当前人数 |
| type | string | 是 | 否 | `instant` / `scheduled` |
| start_time | string/null | 是 | 是 | 预约时间 |
| status | string | 是 | 否 | `waiting` / `ready` / `closed` |
| create_time | string | 是 | 否 | 创建时间 |
| is_owner | boolean | 是 | 否 | 当前用户是否为房主，衍生字段 |

### 2.5 RoomMember

| 字段 | 类型 | 必填 | 可空 | 说明 |
|------|------|------|------|------|
| user_id | long | 是 | 否 | 用户 ID |
| nickname | string | 是 | 否 | 用户昵称 |
| avatar | string | 是 | 否 | 用户头像 URL，可为空字符串 |
| join_time | string | 是 | 否 | 加入时间 |

### 2.6 RoomDetail

| 字段 | 类型 | 必填 | 可空 | 说明 |
|------|------|------|------|------|
| id | long | 是 | 否 | 房间 ID |
| game_id | int | 是 | 否 | 游戏 ID |
| game_name | string | 是 | 否 | 游戏名称 |
| owner_id | long | 是 | 否 | 房主用户 ID |
| owner_nickname | string | 是 | 否 | 房主昵称 |
| max_player | int | 是 | 否 | 最大人数 |
| current_player | int | 是 | 否 | 当前人数 |
| type | string | 是 | 否 | `instant` / `scheduled` |
| start_time | string/null | 是 | 是 | 预约时间 |
| status | string | 是 | 否 | `waiting` / `ready` / `closed` |
| create_time | string | 是 | 否 | 创建时间 |
| update_time | string | 是 | 否 | 更新时间 |
| is_owner | boolean | 是 | 否 | 当前用户是否为房主 |
| is_joined | boolean | 是 | 否 | 当前用户是否在房间中 |
| members | array\<RoomMember> | 是 | 否 | 房间成员列表 |

### 2.7 Message

| 字段 | 类型 | 必填 | 可空 | 说明 |
|------|------|------|------|------|
| id | long | 是 | 否 | 消息 ID |
| room_id | long | 是 | 否 | 房间 ID |
| user_id | long | 是 | 否 | 发送者用户 ID |
| nickname | string | 是 | 否 | 发送者昵称，查询时关联 `t_user` |
| avatar | string | 是 | 否 | 发送者头像 URL，查询时关联 `t_user` |
| content | string | 是 | 否 | 消息内容，1-500 字符 |
| create_time | string | 是 | 否 | 发送时间 |

### 2.8 SmsCode

仅后端内部使用，不直接对前端返回。

| 字段 | 类型 | 必填 | 可空 | 说明 |
|------|------|------|------|------|
| id | long | 是 | 否 | 主键 |
| phone | string | 是 | 否 | 手机号 |
| sms_code | string | 是 | 否 | 6 位验证码 |
| expired_at | string | 是 | 否 | 过期时间 |
| used_status | int | 是 | 否 | `0=未使用`，`1=已使用` |
| used_time | string/null | 是 | 是 | 使用时间 |

---

## 三、状态、一致性与执行顺序

### 3.1 房间状态定义

| 状态 | 含义 | 可否加入 | 可否发消息 |
|------|------|----------|------------|
| waiting | 招募中 | 是 | 是 |
| ready | 已满 | 否 | 是 |
| closed | 已关闭 | 否 | 否 |

### 3.2 失效房间判定

| 房间类型 | 失效条件 |
|----------|----------|
| instant | `create_time + 2 小时 < 当前时间` |
| scheduled | `start_time + 1 小时 < 当前时间` |

说明：
- “失效”不要求先物理删除才算失效
- 接口在查询到房间已失效时，必须按本文档规则先判定不可用，再决定是否同步清理

### 3.3 清理动作定义

清理动作固定为：
1. 将 `t_room.status` 更新为 `closed`
2. 删除 `t_room_user` 中该房间全部成员记录
3. 删除 `t_message` 中该房间全部消息记录

清理后结果：
- 该房间不再出现在房间列表、我的房间列表、房间详情、聊天记录中
- 对该房间的加入、发消息、查询详情、查询聊天记录统一返回 `404`

### 3.4 一人一房约束

规则固定为：
- 同一用户同时最多只能存在于 1 个 `status != closed` 且未失效的房间中
- 已失效但尚未清理的房间，不得继续占用“一人一房”资格
- 创建房间、加入房间前必须先处理该用户关联的失效房间，再做一人一房校验

### 3.5 固定执行顺序

以下顺序为阻塞项，必须按顺序执行，不得交换：

#### 场景 A：创建房间

1. 锁定当前用户数据行：`t_user`
2. 查询当前用户关联的未关闭房间
3. 对查询到的房间逐个执行失效判定
4. 若房间已失效但未清理，立即执行清理动作
5. 清理完成后，再重新判断当前用户是否仍处于有效房间中
6. 若仍存在有效房间，返回 `409 您已在其他未关闭房间中`
7. 若不存在有效房间，继续创建新房间并插入房主成员记录

#### 场景 B：加入房间

1. 锁定当前用户数据行：`t_user`
2. 查询当前用户关联的未关闭房间
3. 对这些房间逐个执行失效判定
4. 若房间已失效但未清理，立即执行清理动作
5. 清理完成后，再重新判断当前用户是否仍处于其他有效房间中
6. 若仍存在其他有效房间，返回 `409 您已在其他未关闭房间中`
7. 再锁定目标房间记录：`t_room`
8. 对目标房间执行失效判定
9. 若目标房间已失效，立即执行清理动作并返回 `404 房间不存在或已失效`
10. 若目标房间有效，再执行人数、成员重复、状态校验并完成加入

#### 场景 C：获取我的房间

1. 查询当前用户关联的未关闭房间
2. 对查询结果逐个执行失效判定
3. 若房间已失效但未清理，立即执行清理动作
4. 清理完成后，再返回剩余有效房间列表
5. 若全部被清理，返回空列表，不得因旧房间残留导致后续创建房间或加入房间被 `409` 卡死

### 3.6 其他一致性规则

- `t_room.current_player` 必须等于 `t_room_user` 实际成员数
- 房主必须始终存在于 `t_room_user`
- `status=ready` 必须满足 `current_player = max_player`
- `status=waiting` 必须满足 `current_player < max_player`
- `status=closed` 的房间不可加入、不可发送消息、不可查询有效详情、不可查询聊天记录
- 房主退出房间等同解散房间

### 3.7 必须使用事务的接口

- 验证码登录
- 创建房间
- 加入房间
- 退出房间
- 解散房间
- 失效房间同步清理
- 发送消息

---

## 四、前后端与数据库打通规则

### 4.1 字段来源

| 字段 | 来源 |
|------|------|
| game_name | `t_game.game_name` |
| owner_nickname | `t_user.nickname` |
| message.nickname | `t_user.nickname` |
| message.avatar | `t_user.avatar` |
| is_joined | 当前用户是否存在于 `t_room_user` |
| is_owner | `t_room.owner_id = current_user_id` |

### 4.2 可空约束

| 字段 | 规则 |
|------|------|
| room.start_time | `type=instant` 时必须为 `null` |
| user.avatar | 不可返回 `null`，允许空字符串 |
| message.avatar | 不可返回 `null`，允许空字符串 |
| 无内容成功返回 | `data=null` |
| 失败返回 | `data=null` |

### 4.3 接口与表映射矩阵

| 接口 | 读取表 | 写入/更新/删除表 |
|------|--------|------------------|
| 发送验证码 | `t_sms_code` | `t_sms_code` |
| 验证码登录 | `t_sms_code`, `t_user` | `t_sms_code`, `t_user` |
| 获取当前用户信息 | `t_user` | - |
| 修改用户信息 | `t_user` | `t_user` |
| 获取游戏列表 | `t_game` | - |
| 创建房间 | `t_user`, `t_game`, `t_room`, `t_room_user` | `t_room`, `t_room_user` |
| 获取房间列表 | `t_room`, `t_game`, `t_user`, `t_room_user` | 必要时清理 `t_room`, `t_room_user`, `t_message` |
| 获取房间详情 | `t_room`, `t_game`, `t_user`, `t_room_user` | 必要时清理 `t_room`, `t_room_user`, `t_message` |
| 加入房间 | `t_user`, `t_room`, `t_room_user` | `t_room`, `t_room_user`, 必要时清理 `t_message` |
| 退出房间 | `t_room`, `t_room_user` | `t_room`, `t_room_user`, `t_message` |
| 解散房间 | `t_room` | `t_room`, `t_room_user`, `t_message` |
| 获取我的房间 | `t_room`, `t_room_user`, `t_game`, `t_user` | 必要时清理 `t_room`, `t_room_user`, `t_message` |
| 发送消息 | `t_room`, `t_room_user`, `t_user` | `t_message`, 必要时清理 `t_room`, `t_room_user`, `t_message` |
| 获取聊天记录 | `t_room`, `t_room_user`, `t_message`, `t_user` | 必要时清理 `t_room`, `t_room_user`, `t_message` |

---

## 五、登录模块

### 5.1 发送验证码

**接口用途**: 发送短信验证码  
**请求方式**: `POST`  
**请求路径**: `/api/auth/sms/send`  
**鉴权要求**: 无

**请求参数**

| 参数名 | 类型 | 必填 | 可空 | 说明 |
|--------|------|------|------|------|
| phone | string | 是 | 否 | 11 位手机号 |

**请求示例**

```json
{
  "phone": "13812345678"
}
```

**返回体定义**

`data` 为 `SendSmsResponse`：

| 字段 | 类型 | 必填 | 可空 | 说明 |
|------|------|------|------|------|
| expires_in | int | 是 | 否 | 验证码剩余有效秒数，固定返回 `300` |

**成功返回**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "expires_in": 300
  }
}
```

**业务规则**

- 同一手机号最短发送间隔 60 秒
- 验证码有效期 300 秒
- 同一手机号再次发送时，新的验证码覆盖旧的未使用验证码

**业务错误场景**

| 场景 | code | message |
|------|------|---------|
| 手机号格式错误 | 400 | 参数不合法 |
| 发送过于频繁 | 409 | 验证码发送过于频繁，请稍后再试 |

**数据库影响**

- 读取：`t_sms_code`
- 写入/更新：`t_sms_code`

**事务要求**

- 单表写入，可不单独开启事务

---

### 5.2 验证码登录

**接口用途**: 验证手机号并完成登录  
**请求方式**: `POST`  
**请求路径**: `/api/auth/login`  
**鉴权要求**: 无

**请求参数**

| 参数名 | 类型 | 必填 | 可空 | 说明 |
|--------|------|------|------|------|
| phone | string | 是 | 否 | 11 位手机号 |
| code | string | 是 | 否 | 6 位验证码 |

**请求示例**

```json
{
  "phone": "13812345678",
  "code": "123456"
}
```

**返回体定义**

`data` 为 `LoginResponse`：

| 字段 | 类型 | 必填 | 可空 | 说明 |
|------|------|------|------|------|
| token | string | 是 | 否 | JWT access token |
| expires_in | int | 是 | 否 | token 剩余有效秒数，固定返回 `604800` |
| user | object | 是 | 否 | 当前登录用户，结构见 `User` |

**成功返回**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.xxx",
    "expires_in": 604800,
    "user": {
      "id": 10001,
      "phone": "13812345678",
      "nickname": "玩家0001",
      "avatar": "",
      "credit_score": 100
    }
  }
}
```

**业务规则**

- 手机号首次登录时自动注册用户
- 默认昵称：`玩家 + 用户ID后4位`
- 默认头像：`""`
- 默认信用分：`100`
- 登录成功后验证码必须立即标记为已使用

**业务错误场景**

| 场景 | code | message |
|------|------|---------|
| 验证码错误 | 400 | 验证码错误 |
| 验证码已过期 | 400 | 验证码已过期，请重新获取 |
| 手机号格式错误 | 400 | 参数不合法 |

**数据库影响**

- 读取/更新：`t_sms_code`
- 读取/新增：`t_user`

**事务要求**

- 必须使用事务，保证“校验验证码 + 标记已使用 + 首次登录注册用户”一致

---

### 5.3 获取当前用户信息

**接口用途**: 获取当前登录用户资料  
**请求方式**: `GET`  
**请求路径**: `/api/auth/me`  
**鉴权要求**: 需要 Token

**返回体定义**

`data` 为 `User`。

**成功返回**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 10001,
    "phone": "13812345678",
    "nickname": "玩家0001",
    "avatar": "",
    "credit_score": 100
  }
}
```

**数据库影响**

- 读取：`t_user`

**事务要求**

- 无

---

## 六、用户与公共字典模块

### 6.1 修改用户信息

**接口用途**: 修改昵称与头像  
**请求方式**: `PUT`  
**请求路径**: `/api/users/profile`  
**鉴权要求**: 需要 Token

**请求参数**

| 参数名 | 类型 | 必填 | 可空 | 说明 |
|--------|------|------|------|------|
| nickname | string | 条件必填 | 否 | 昵称，2-20 字符 |
| avatar | string | 条件必填 | 否 | 头像 URL，可传空字符串清空 |

规则：
- 至少传一个字段
- `nickname` 不允许为空字符串，不允许换行和制表符
- `avatar` 非空时必须以 `http://` 或 `https://` 开头

**成功返回**

`data` 为 `User`。

**业务错误场景**

| 场景 | code | message |
|------|------|---------|
| 请求体为空 | 400 | 参数不合法 |
| nickname 格式错误 | 400 | 参数不合法 |
| avatar 格式错误 | 400 | 参数不合法 |

**数据库影响**

- 更新：`t_user`

**事务要求**

- 无

---

### 6.2 获取游戏列表

**接口用途**: 获取前端可展示、可创建房间使用的游戏字典  
**请求方式**: `GET`  
**请求路径**: `/api/games`  
**鉴权要求**: 需要 Token

**请求参数**

无。

**返回体定义**

`data` 为数组，元素类型为 `Game`。

**成功返回**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "game_name": "王者荣耀",
      "status": "enabled"
    },
    {
      "id": 2,
      "game_name": "英雄联盟",
      "status": "enabled"
    }
  ]
}
```

**业务规则**

- 仅返回 `status=enabled` 的游戏
- 排序固定为 `sort_no ASC, id ASC`
- 前端创建房间时应以后端返回的 `id` 为准

**数据库影响**

- 读取：`t_game`

**事务要求**

- 无

---

## 七、房间模块

### 7.1 创建房间

**接口用途**: 创建约玩房间  
**请求方式**: `POST`  
**请求路径**: `/api/rooms`  
**鉴权要求**: 需要 Token

**请求参数**

| 参数名 | 类型 | 必填 | 可空 | 说明 |
|--------|------|------|------|------|
| game_id | int | 是 | 否 | 游戏 ID，必须存在且可用 |
| max_player | int | 是 | 否 | 最大人数，取值 `2-10` |
| type | string | 是 | 否 | `instant` / `scheduled` |
| start_time | string | 条件必填 | 是 | `type=scheduled` 时必填 |

规则：
- `type=instant` 时，`start_time` 必须不传或传 `null`
- `type=scheduled` 时，`start_time` 必须大于当前时间，且不超过当前时间 + 1 天
- 执行顺序必须遵循 [三、状态、一致性与执行顺序](#三状态一致性与执行顺序)

**请求示例**

```json
{
  "game_id": 1,
  "max_player": 5,
  "type": "instant"
}
```

**返回体定义**

`data` 为 `RoomDetail`。

**成功返回**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 20001,
    "game_id": 1,
    "game_name": "王者荣耀",
    "owner_id": 10001,
    "owner_nickname": "玩家0001",
    "max_player": 5,
    "current_player": 1,
    "type": "instant",
    "start_time": null,
    "status": "waiting",
    "create_time": "2026-04-17 15:00:00",
    "update_time": "2026-04-17 15:00:00",
    "is_owner": true,
    "is_joined": true,
    "members": [
      {
        "user_id": 10001,
        "nickname": "玩家0001",
        "avatar": "",
        "join_time": "2026-04-17 15:00:00"
      }
    ]
  }
}
```

**业务错误场景**

| 场景 | code | message |
|------|------|---------|
| 用户已在其他有效房间 | 409 | 您已在其他未关闭房间中 |
| `start_time` 不合法 | 400 | 参数不合法 |
| `instant` 传了 `start_time` | 400 | 参数不合法 |
| `game_id` 不存在或不可用 | 400 | 参数不合法 |

**数据库影响**

- 读取：`t_user`, `t_game`, `t_room`, `t_room_user`
- 写入：`t_room`, `t_room_user`
- 必要时清理：`t_room`, `t_room_user`, `t_message`

**事务要求**

- 必须使用事务

---

### 7.2 获取房间列表

**接口用途**: 获取可加入房间列表  
**请求方式**: `GET`  
**请求路径**: `/api/rooms`  
**鉴权要求**: 需要 Token

**请求参数**

| 参数名 | 类型 | 必填 | 可空 | 说明 |
|--------|------|------|------|------|
| game_id | int | 否 | 否 | 按游戏 ID 过滤 |
| type | string | 否 | 否 | `instant` / `scheduled` |
| status | string | 否 | 否 | `waiting` / `ready`，默认两者 |
| page | int | 否 | 否 | 默认 `1` |
| size | int | 否 | 否 | 默认 `20`，最大 `50` |

排序固定：
- `create_time DESC, id DESC`

**返回体定义**

分页 `list` 元素类型为 `RoomListItem`。

**成功返回**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 100,
    "page": 1,
    "size": 20,
    "list": [
      {
        "id": 20001,
        "game_id": 1,
        "game_name": "王者荣耀",
        "owner_id": 10001,
        "owner_nickname": "玩家0001",
        "max_player": 5,
        "current_player": 3,
        "type": "instant",
        "start_time": null,
        "status": "waiting",
        "create_time": "2026-04-17 15:00:00",
        "is_joined": false
      }
    ]
  }
}
```

**业务规则**

- 仅返回有效且 `status in (waiting, ready)` 的房间
- 查询时若发现房间已失效但未清理，可同步清理后不返回该房间
- `game_name`、`owner_nickname` 均由后端查询拼装

**数据库影响**

- 读取：`t_room`, `t_game`, `t_user`, `t_room_user`
- 必要时清理：`t_room`, `t_room_user`, `t_message`

**事务要求**

- 纯查询可不包事务；若执行同步清理，清理动作需单独事务完成

---

### 7.3 获取房间详情

**接口用途**: 查看房间详情与成员  
**请求方式**: `GET`  
**请求路径**: `/api/rooms/{roomId}`  
**鉴权要求**: 需要 Token

**路径参数**

| 参数名 | 类型 | 必填 | 可空 | 说明 |
|--------|------|------|------|------|
| roomId | long | 是 | 否 | 房间 ID，正整数 |

**返回体定义**

`data` 为 `RoomDetail`。

**成功返回**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 20001,
    "game_id": 1,
    "game_name": "王者荣耀",
    "owner_id": 10001,
    "owner_nickname": "玩家0001",
    "max_player": 5,
    "current_player": 3,
    "type": "instant",
    "start_time": null,
    "status": "waiting",
    "create_time": "2026-04-17 15:00:00",
    "update_time": "2026-04-17 15:00:00",
    "is_owner": false,
    "is_joined": true,
    "members": [
      {
        "user_id": 10001,
        "nickname": "玩家0001",
        "avatar": "",
        "join_time": "2026-04-17 15:00:00"
      }
    ]
  }
}
```

**业务规则**

- `members` 排序固定为：房主优先，其余按 `join_time ASC`
- 若房间已失效但未清理，应先清理再返回 `404`

**业务错误场景**

| 场景 | code | message |
|------|------|---------|
| 房间不存在或已失效 | 404 | 房间不存在或已失效 |

**数据库影响**

- 读取：`t_room`, `t_game`, `t_user`, `t_room_user`
- 必要时清理：`t_room`, `t_room_user`, `t_message`

**事务要求**

- 纯查询可不包事务；若执行同步清理，清理动作需单独事务完成

---

### 7.4 加入房间

**接口用途**: 加入指定房间  
**请求方式**: `POST`  
**请求路径**: `/api/rooms/{roomId}/join`  
**鉴权要求**: 需要 Token

**路径参数**

| 参数名 | 类型 | 必填 | 可空 | 说明 |
|--------|------|------|------|------|
| roomId | long | 是 | 否 | 目标房间 ID，正整数 |

**返回体定义**

`data` 为 `RoomDetail`。

**业务规则**

- 执行顺序必须遵循 [3.5 固定执行顺序](#35-固定执行顺序)
- 加入成功后必须新增 `t_room_user`
- 加入成功后必须同步更新 `t_room.current_player`
- 若加入后人数达到 `max_player`，房间状态更新为 `ready`

**业务错误场景**

| 场景 | code | message |
|------|------|---------|
| 房间不存在或已失效 | 404 | 房间不存在或已失效 |
| 用户已在该房间中 | 409 | 您已在该房间中 |
| 用户已在其他有效房间 | 409 | 您已在其他未关闭房间中 |
| 房间已满 | 409 | 房间已满，无法加入 |
| 房间已关闭 | 404 | 房间不存在或已失效 |

**数据库影响**

- 读取：`t_user`, `t_room`, `t_room_user`
- 写入/更新：`t_room`, `t_room_user`
- 必要时清理：`t_room`, `t_room_user`, `t_message`

**事务要求**

- 必须使用事务

---

### 7.5 退出房间

**接口用途**: 当前成员退出房间  
**请求方式**: `POST`  
**请求路径**: `/api/rooms/{roomId}/leave`  
**鉴权要求**: 需要 Token

**路径参数**

| 参数名 | 类型 | 必填 | 可空 | 说明 |
|--------|------|------|------|------|
| roomId | long | 是 | 否 | 目标房间 ID，正整数 |

**返回体定义**

`data` 为 `LeaveRoomResponse`：

| 字段 | 类型 | 必填 | 可空 | 说明 |
|------|------|------|------|------|
| action | string | 是 | 否 | `left`=普通成员退出成功；`room_closed`=房主退出并触发解散 |

**成功返回**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "action": "left"
  }
}
```

**业务规则**

- 普通成员退出：删除 `t_room_user`，同步减少 `current_player`
- 普通成员退出后若 `current_player < max_player`，房间状态为 `waiting`
- 房主退出：等同解散房间，返回 `action=room_closed`

**业务错误场景**

| 场景 | code | message |
|------|------|---------|
| 房间不存在或已失效 | 404 | 房间不存在或已失效 |
| 用户不在房间中 | 403 | 您不在该房间中 |

**数据库影响**

- 读取：`t_room`, `t_room_user`
- 更新/删除：`t_room`, `t_room_user`
- 房主退出时额外删除：`t_message`

**事务要求**

- 必须使用事务

---

### 7.6 解散房间

**接口用途**: 房主解散房间  
**请求方式**: `DELETE`  
**请求路径**: `/api/rooms/{roomId}`  
**鉴权要求**: 需要 Token

**路径参数**

| 参数名 | 类型 | 必填 | 可空 | 说明 |
|--------|------|------|------|------|
| roomId | long | 是 | 否 | 目标房间 ID，正整数 |

**返回体定义**

`data` 固定为 `null`。

**成功返回**

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

**业务规则**

- 仅房主可解散房间
- 解散动作固定为：更新 `t_room.status=closed`，删除 `t_room_user`，删除 `t_message`
- 若房间已 `closed`，重复调用仍返回成功，视为幂等

**业务错误场景**

| 场景 | code | message |
|------|------|---------|
| 房间不存在 | 404 | 房间不存在或已失效 |
| 非房主解散 | 403 | 仅房主可解散房间 |

**数据库影响**

- 更新：`t_room`
- 删除：`t_room_user`, `t_message`

**事务要求**

- 必须使用事务

---

### 7.7 获取我的房间

**接口用途**: 获取当前用户所在的有效房间  
**请求方式**: `GET`  
**请求路径**: `/api/rooms/my`  
**鉴权要求**: 需要 Token

**请求参数**

| 参数名 | 类型 | 必填 | 可空 | 说明 |
|--------|------|------|------|------|
| status | string | 否 | 否 | `waiting` / `ready`，默认两者 |
| page | int | 否 | 否 | 默认 `1` |
| size | int | 否 | 否 | 默认 `20`，最大 `50` |

**返回体定义**

分页 `list` 元素类型为 `RoomMyItem`。

**成功返回**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 1,
    "page": 1,
    "size": 20,
    "list": [
      {
        "id": 20001,
        "game_id": 1,
        "game_name": "王者荣耀",
        "owner_id": 10001,
        "owner_nickname": "玩家0001",
        "max_player": 5,
        "current_player": 3,
        "type": "instant",
        "start_time": null,
        "status": "waiting",
        "create_time": "2026-04-17 15:00:00",
        "is_owner": false
      }
    ]
  }
}
```

**业务规则**

- 执行顺序必须遵循 [3.5 固定执行顺序](#35-固定执行顺序)
- 返回值包含当前用户作为房主创建的房间
- 若查询结果中的房间已失效但未清理，必须先清理，再返回剩余有效房间
- `page` 超范围返回空数组

**数据库影响**

- 读取：`t_room`, `t_room_user`, `t_game`, `t_user`
- 必要时清理：`t_room`, `t_room_user`, `t_message`

**事务要求**

- 纯查询可不包事务；若执行同步清理，清理动作需单独事务完成

---

## 八、聊天模块

### 8.1 发送消息

**接口用途**: 在房间内发送消息  
**请求方式**: `POST`  
**请求路径**: `/api/rooms/{roomId}/messages`  
**鉴权要求**: 需要 Token

**路径参数**

| 参数名 | 类型 | 必填 | 可空 | 说明 |
|--------|------|------|------|------|
| roomId | long | 是 | 否 | 目标房间 ID，正整数 |

**请求参数**

| 参数名 | 类型 | 必填 | 可空 | 说明 |
|--------|------|------|------|------|
| content | string | 是 | 否 | 消息内容，1-500 字符 |

**请求示例**

```json
{
  "content": "大家好！"
}
```

**返回体定义**

`data` 为 `Message`。

**成功返回**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 30001,
    "room_id": 20001,
    "user_id": 10001,
    "nickname": "玩家0001",
    "avatar": "",
    "content": "大家好！",
    "create_time": "2026-04-17 15:10:00"
  }
}
```

**业务规则**

- 发送前必须校验当前用户属于该房间成员
- 若目标房间已失效但未清理，应先清理再返回 `404`

**业务错误场景**

| 场景 | code | message |
|------|------|---------|
| 房间不存在或已失效 | 404 | 房间不存在或已失效 |
| 用户不在房间中 | 403 | 您不在该房间中 |
| 消息内容为空或超长 | 400 | 参数不合法 |

**数据库影响**

- 读取：`t_room`, `t_room_user`, `t_user`
- 写入：`t_message`
- 必要时清理：`t_room`, `t_room_user`, `t_message`

**事务要求**

- 必须使用事务，保证“成员校验 + 房间有效性校验 + 写消息”一致

---

### 8.2 获取聊天记录

**接口用途**: 获取房间聊天消息  
**请求方式**: `GET`  
**请求路径**: `/api/rooms/{roomId}/messages`  
**鉴权要求**: 需要 Token

**路径参数**

| 参数名 | 类型 | 必填 | 可空 | 说明 |
|--------|------|------|------|------|
| roomId | long | 是 | 否 | 目标房间 ID，正整数 |

**请求参数**

| 参数名 | 类型 | 必填 | 可空 | 说明 |
|--------|------|------|------|------|
| before_id | long | 否 | 否 | 历史游标，不传则返回最新消息 |
| size | int | 否 | 否 | 默认 `50`，最大 `100` |

**分页规则**

- `before_id` 不传时，返回最新 `size` 条消息
- `before_id` 传值时，返回 `id < before_id` 的消息
- 排序固定为 `id DESC`

**返回体定义**

`data` 为 `MessagePageResponse`：

| 字段 | 类型 | 必填 | 可空 | 说明 |
|------|------|------|------|------|
| has_more | boolean | 是 | 否 | 是否还有更早消息 |
| messages | array\<Message> | 是 | 否 | 消息列表，无数据时返回 `[]` |

**成功返回**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "has_more": true,
    "messages": [
      {
        "id": 30001,
        "room_id": 20001,
        "user_id": 10001,
        "nickname": "玩家0001",
        "avatar": "",
        "content": "大家好！",
        "create_time": "2026-04-17 15:10:00"
      }
    ]
  }
}
```

**业务规则**

- 若消息列表为空，返回 `messages=[]`，`has_more=false`
- 仅房间成员可读取聊天记录
- 若目标房间已失效但未清理，应先清理再返回 `404`
- 返回的昵称、头像由后端关联用户表拼装

**业务错误场景**

| 场景 | code | message |
|------|------|---------|
| 房间不存在或已失效 | 404 | 房间不存在或已失效 |
| 用户不在房间中 | 403 | 您不在该房间中 |
| `size` 超出范围 | 400 | 参数不合法 |

**数据库影响**

- 读取：`t_room`, `t_room_user`, `t_message`, `t_user`
- 必要时清理：`t_room`, `t_room_user`, `t_message`

**事务要求**

- 纯查询可不包事务；若执行同步清理，清理动作需单独事务完成

---

## 九、数据库表设计

### 9.1 表关系

| 表名 | 作用 |
|------|------|
| `t_user` | 用户主表 |
| `t_game` | 游戏字典表 |
| `t_sms_code` | 验证码记录表 |
| `t_room` | 房间主表 |
| `t_room_user` | 房间成员关系表 |
| `t_message` | 房间聊天消息表 |

### 9.2 用户表 `t_user`

```sql
CREATE TABLE t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    phone VARCHAR(11) NOT NULL UNIQUE,
    nickname VARCHAR(20) NOT NULL DEFAULT '',
    avatar VARCHAR(255) NOT NULL DEFAULT '',
    credit_score INT NOT NULL DEFAULT 100,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

| 字段 | 类型 | 非空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| phone | VARCHAR(11) | 是 | - | 手机号，唯一 |
| nickname | VARCHAR(20) | 是 | `''` | 昵称 |
| avatar | VARCHAR(255) | 是 | `''` | 头像 URL |
| credit_score | INT | 是 | `100` | 信用分 |

### 9.3 游戏表 `t_game`

```sql
CREATE TABLE t_game (
    id INT PRIMARY KEY,
    game_name VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'enabled',
    sort_no INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status_sort (status, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 9.4 验证码表 `t_sms_code`

```sql
CREATE TABLE t_sms_code (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    phone VARCHAR(11) NOT NULL,
    sms_code VARCHAR(6) NOT NULL,
    expired_at DATETIME NOT NULL,
    used_status TINYINT NOT NULL DEFAULT 0,
    used_time DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_phone_status (phone, used_status),
    INDEX idx_expired_at (expired_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 9.5 房间表 `t_room`

```sql
CREATE TABLE t_room (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    game_id INT NOT NULL,
    owner_id BIGINT NOT NULL,
    max_player INT NOT NULL,
    current_player INT NOT NULL DEFAULT 0,
    type VARCHAR(20) NOT NULL,
    start_time DATETIME DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'waiting',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_game_id (game_id),
    INDEX idx_owner_id (owner_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

字段要求：
- `type` 仅允许 `instant`、`scheduled`
- `status` 仅允许 `waiting`、`ready`、`closed`
- `start_time` 在 `type=instant` 时必须为 `NULL`

### 9.6 房间成员表 `t_room_user`

```sql
CREATE TABLE t_room_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    join_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_room_user (room_id, user_id),
    INDEX idx_room_id (room_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

说明：
- `uk_room_user` 保证同一用户不会重复加入同一房间
- 一人一房约束不依赖该唯一键单独完成，仍需业务校验

### 9.7 聊天消息表 `t_message`

```sql
CREATE TABLE t_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_room_id (room_id),
    INDEX idx_room_id_id (room_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

说明：
- 应用层限制 `content` 最长 500 字符
- 存储层保留更宽长度，避免多字节字符与转义带来截断风险

### 9.8 数据库上线核对项

上线前必须核对：
- `t_game` 已初始化可用游戏数据
- `t_sms_code` 已建表
- `t_room.current_player` 与 `t_room_user` 实际成员数一致
- 所有表字符集统一为 `utf8mb4`
- 所有时间写入均使用服务端北京时间

---

## 十、附录

### 10.1 当前游戏基础数据

| game_id | game_name |
|---------|-----------|
| 1 | 王者荣耀 |
| 2 | 英雄联盟 |
| 3 | 和平精英 |
| 4 | 原神 |
| 5 | 蛋仔派对 |

### 10.2 静态字典使用边界

- 本版已提供 `GET /api/games`，前端应优先使用接口返回结果
- 本附录仅作为初始化测试数据和联调对照表，不应作为前端正式运行时的唯一数据源
- 若附录与 `t_game`、`GET /api/games` 返回不一致，以数据库与接口返回为准

### 10.3 接口清单

| 模块 | 接口 | 方法 | 路径 |
|------|------|------|------|
| 登录 | 发送验证码 | `POST` | `/api/auth/sms/send` |
| 登录 | 验证码登录 | `POST` | `/api/auth/login` |
| 登录 | 获取当前用户 | `GET` | `/api/auth/me` |
| 用户 | 修改用户信息 | `PUT` | `/api/users/profile` |
| 公共字典 | 获取游戏列表 | `GET` | `/api/games` |
| 房间 | 创建房间 | `POST` | `/api/rooms` |
| 房间 | 获取房间列表 | `GET` | `/api/rooms` |
| 房间 | 获取房间详情 | `GET` | `/api/rooms/{roomId}` |
| 房间 | 加入房间 | `POST` | `/api/rooms/{roomId}/join` |
| 房间 | 退出房间 | `POST` | `/api/rooms/{roomId}/leave` |
| 房间 | 解散房间 | `DELETE` | `/api/rooms/{roomId}` |
| 房间 | 获取我的房间 | `GET` | `/api/rooms/my` |
| 聊天 | 发送消息 | `POST` | `/api/rooms/{roomId}/messages` |
| 聊天 | 获取聊天记录 | `GET` | `/api/rooms/{roomId}/messages` |

### 10.4 版本说明

v2.4.1 最终联调上线版相对上一稿的本次整理重点：
- 将文档主体改为以接口定义为中心，弱化流程角色描述
- 固定“失效判定 -> 清理动作 -> 一人一房校验”的执行顺序
- 明确创建房间、加入房间、获取我的房间 3 个场景对“已失效但未清理房间”的处理
- 为 `7.4`、`7.5`、`7.6`、`8.1`、`8.2` 补齐显式 `roomId` 路径参数定义
- 为 `5.1`、`5.2`、`7.5`、`8.2` 补齐正式返回体字段定义
- 新增 `GET /api/games`，避免前端长期依赖附录静态字典

