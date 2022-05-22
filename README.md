# raft
## 1.raft算法简介
Raft 是一种共识算法，其特点是让多个参与者针对某一件事达成完全一致：一件事，一个结论。同时对已达成一致的结论，是不可推翻的。可以举一个银行账户的例子来解释共识算法：假如由一批服务器组成一个集群来维护银行账户系统，如果有一个 Client 向集群发出“存 100 元”的指令，那么当集群返回成功应答之后，Client 再向集群发起查询时，一定能够查到被存储成功的这 100 元钱，就算有机器出现不可用情况，这 100 元的账也不可篡改。这就是共识算法要达到的效果。

Raft 算法和其他的共识算法相比，又有了如下几个不同的特性：

Strong leader：Raft 集群中最多只能有一个 Leader，日志只能从 Leader 复制到 Follower 上；
Leader election：Raft 算法采用随机选举超时时间触发选举来避免选票被瓜分的情况，保证选举的顺利完成；
Membership changes：通过两阶段的方式应对集群内成员的加入或者退出情况，在此期间并不影响集群对外的服务。
共识算法有一个很典型的应用场景就是复制状态机。Client 向复制状态机发送一系列能够在状态机上执行的命令，共识算法负责将这些命令以 Log 的形式复制给其他的状态机，这样不同的状态机只要按照完全一样的顺序来执行这些命令，就能得到一样的输出结果。所以这就需要利用共识算法保证被复制日志的内容和顺序一致。

![image](https://user-images.githubusercontent.com/105574077/169689390-647b88f8-6056-4694-9768-2dc68426f05c.png)

### 1.1 leader选举
 复制状态机集群在利用 Raft 算法保证一致性时，要做的第一件事情就是 Leader 选举。在讲 Leader 选举之前我们先要说一个重要的概念：Term。Term 用来将一个连续的时间轴在逻辑上切割成一个个区间，它的含义类似于“美国第 26 届总统”这个表述中的“26”。
 ![image](https://user-images.githubusercontent.com/105574077/169689417-bbe290fa-fc9a-4d6e-90b4-afb7bccfba92.png)

每一个 Term 期间集群要做的第一件事情就是选举 Leader。起初所有的 Server 都是 Follower 角色，如果 Follower 经过一段时间( election timeout )的等待却依然没有收到其他 Server 发来的消息时，Follower 就可以认为集群中没有可用的 Leader，遂开始准备发起选举。在发起选举的时候 Server 会从 Follower 角色转变成 Candidate，然后开始尝试竞选 Term + 1 届的 Leader，此时他会向其他的 Server 发送投票请求，当收到集群内多数机器同意其当选的应答之后，Candidate 成功当选 Leader。但是如下两种情况会让 Candidate 退回 (step down) 到 Follower，放弃竞选本届 Leader：

如果在 Candidate 等待 Servers 的投票结果期间收到了其他拥有更高 Term 的 Server 发来的投票请求；

如果在 Candidate 等待 Servers 的投票结果期间收到了其他拥有更高 Term 的 Server 发来的心跳；

当然了，当一个 Leader 发现有 Term 更高的 Leader 时也会退回到 Follower 状态。

当选举 Leader 成功之后，整个集群就可以向外提供正常读写服务了，如图所示，集群由一个 Leader 两个 Follower 组成，Leader 负责处理 Client 发起的读写请求，同时还要跟 Follower 保持心跳或者把 Log 复制给 Follower。

### 1.2 log复制
下面我们就详细说一下 Log 复制。我们之前已经说了 Log 就是 Client 发送给复制状态机的一系列命令。这里我们再举例解释一下 Log，比如我们的复制状态机要实现的是一个银行账户系统，那么这个 Log 就可以是 Client 发给账户系统的一条存钱的命令，比如“存 100 元钱”。

Leader 与 Follower 之间的日志复制是共识算法运用于复制状态机的重要目的，在 Raft 算法中 Log 由 TermId、LogIndex、LogValue 这三要素构成，在这张图上每一个小格代表一个 Log。当 Leader 在向 Follower 复制 Log 的时候，Follower 还需要对收到的 Log 做检查，以确保这些 Log 能和本地已有的 Log 保持连续。我们之前说了，Raft 算法是要严格保证 Log 的连续性的，所以 Follower 会拒绝无法和本地已有 Log 保持连续的复制请求，那么这种情况下就需要走 Log 恢复的流程。总之，Log 复制的目的就是要让所有的 Server 上的 Log 无论在内容上还是在顺序上都要保持完全一致，这样才能保证所有状态机执行结果一致。

![image](https://user-images.githubusercontent.com/105574077/169689553-f0e29f96-493f-4ffa-aea0-24c4ed2534fd.png)



## 2，实现Raft算法

### 2.1 实现功能
主要实现单leader的raft集群算法，multi-group模式后续再实现
 （1）leader选举
	（2）日志复制
	（3）日志快照
	（4）集群成员管理
	
### 2.2 组件图

![image](https://user-images.githubusercontent.com/105574077/169656408-1ef0b3c0-5e50-4668-85aa-f159560621f1.png)
