# AE2-Virus 设计文档

适用版本：Minecraft 1.21.1 NeoForge  
依赖方向：Applied Energistics 2  
Mod 名称：AE2-Virus

## 1. 核心概念

AE2-Virus 的目标是为 AE2 网络加入“网络安全”和“病毒入侵”的玩法元素。玩家在扩展 ME 网络时，不再只需要考虑频道、电力、存储容量和自动化效率，也需要考虑网络暴露面、设备安全等级、病毒扩散风险和灾后恢复能力。

网络病毒是一种可以侵染 AE2 网络的异常状态。病毒存在于网络中时，会逐渐干扰网络内物品的正常访问，并可能造成物品封锁、损坏、丢失等后果。病毒不是单纯的负面随机事件，而是一个围绕“风险管理、网络隔离、防御建设、主动杀毒”的系统。

## 2. 设计目标

- 让 AE2 网络建设拥有安全维度，鼓励玩家设计更严谨的线缆和设备布局。
- 让玻璃线缆、包层线缆、智能线缆等线缆类型产生更明确的安全差异。
- 为大型自动化基地加入可预防、可监控、可处理的长期风险。
- 避免病毒机制变成纯粹惩罚，提供防火墙、扫描、杀毒、恢复等反制手段。
- 保留 AE2 的核心体验：高效存储、自动化、网络规划，同时加入新的策略层。

## 3. 网络病毒

网络病毒是寄生在 AE2 网络中的异常程序。它不会直接破坏方块，而是影响网络对物品的索引、取出、存入和存储安全。

病毒可以具有以下状态：

- 潜伏：病毒已经进入网络，但尚未触发明显影响。此阶段适合通过扫描提前发现。
- 活跃：病毒开始封锁物品，玩家能够明显感受到网络异常。
- 扩散：病毒影响范围扩大，可能从单个物品、单个磁盘或单个设备扩展到更大范围。
- 破坏：被封锁的物品开始进入损坏和丢失流程。
- 清除：病毒被防火墙杀毒机制移除，封锁状态解除。

病毒可以按照感染谱系和威胁强度分为多个等级。命名可以参考医学中的“靶向、广谱”和计算机安全中的“系统级、多态”概念：

- 靶向型病毒：只针对某一种具体物品，封锁持续时间短，损坏概率低，对应“专一性病毒”的定位。
- 广谱型病毒：可影响同类物品、标签、磁盘或设备，具有一定扩散能力，对应“光谱性病毒”的定位。
- 系统型病毒：可影响整个 AE2 网络，损坏和丢失速度更快，需要主动杀毒处理，对应“超级病毒”的定位。
- 多态型病毒：拥有特殊规则或变异行为，例如优先攻击高价值物品、自动化输入物品或指定标签物品，对应“特殊病毒”的定位。

## 4. 物品封锁

物品封锁是病毒最主要的表现形式。被封锁的物品无法从 AE2 网络中取出，但仍然可以继续存入。新存入的同类物品如果符合封锁条件，也会自动进入封锁状态。

### 4.1 封锁维度

病毒可以从多个维度封锁物品：

- 数量封锁：只封锁某个物品的一部分数量。例如网络中有 1000 个铁锭，病毒只封锁其中 300 个。
- 种类封锁：封锁某一种具体物品。例如所有铁锭都无法取出。
- 标签封锁：封锁某个物品标签下的所有物品。例如所有 `forge:ingots` 或所有矿物类物品。
- 磁盘封锁：封锁某个 ME 存储元件内的全部内容。
- 驱动器封锁：封锁某个 ME Drive 中所有磁盘内的物品。
- 网络封锁：封锁整个 AE2 网络中的部分或全部物品，是最严重的情况。
- 通道封锁：封锁经由某一段线缆、某个子网或某个接口可访问的物品。
- 自动化封锁：优先封锁由输入总线、输出总线、样板供应器、接口等自动化设备处理的物品。

### 4.2 封锁表现

玩家在终端中查看被封锁物品时，可以看到异常提示，例如：

- 状态：已封锁
- 原因：网络病毒干扰
- 风险：即将损坏
- 建议：使用防火墙进行扫描和杀毒

被封锁物品的交互规则：

- 无法手动取出。
- 无法被输出总线取出。
- 无法被合成系统正常消耗。
- 可以继续存入。
- 存入后会合并进入封锁数量。
- 如果封锁来自磁盘、驱动器或网络层级，则物品即使种类不同也可能受影响。

### 4.3 损坏与丢失

封锁不是最终状态。物品被封锁一段时间后，会进入损坏阶段；损坏物品继续停留一段时间后，可能进入丢失阶段。

推荐流程：

1. 正常物品被病毒封锁。
2. 封锁持续一定时间后，物品变为损坏物品。
3. 损坏持续一定时间后，物品有概率丢失。
4. 病毒被清除后，封锁解除，部分损坏物品有概率恢复。
5. 已经丢失的物品无法恢复。

损坏状态可以有多种设计方式：

- 物品仍显示在终端中，但带有损坏标记。
- 物品数量保持不变，但不可用于合成或取出。
- 物品有概率恢复，有概率继续恶化。
- 损坏物品可以作为后续玩法材料，例如“数据残片”“损坏数据包”“污染物品索引”。

丢失状态代表该部分物品已经从网络中永久消失。为了避免过度惩罚，建议丢失发生前提供明显预警，并允许玩家通过防火墙及时干预。

## 5. 侵染机制

病毒入侵的核心来源是“暴露面”。玩家的 AE2 网络越开放、越使用裸露设备和玻璃线缆，病毒入侵概率越高；越注重包覆、隔离和防火墙保护，风险越低。

### 5.1 侵染概率函数

侵染概率建议拆成两个阶段：

1. 是否发生一次侵染尝试：由病毒类型对应的网络压力值决定。
2. 这次侵染尝试是否成功：由该网络当前暴露面决定。

这样可以让“病毒是否找到值得攻击的目标”和“暴露设计是否允许病毒侵入”分别承担不同职责。网络中某类资源越多，越容易吸引对应类型病毒尝试侵染；网络很大但完全封闭时，会频繁被尝试扫描但很难成功；网络很小但大量暴露时，尝试次数少，但一旦尝试就更容易成功。

四类病毒的尝试压力不同：

- 靶向型病毒：根据目标物在网络中的数量计算尝试概率。
- 广谱型病毒：分为标签广谱、磁盘广谱、驱动器广谱三种变体，分别根据标签完整度、磁盘字节和被感染磁盘数量计算尝试概率，并受网络中已有靶向型病毒数量影响。
- 系统型病毒：根据网络总字节数和网络中已有广谱型病毒数量计算尝试概率，网络总字节数包含已使用字节和未使用字节。
- 多态型病毒：根据病毒生产黑名单物品在网络中的数量计算尝试概率。

这让病毒具备“进化链”：

- 靶向型病毒从具体物品大量堆积中产生。
- 广谱型病毒从同类资源聚集、存储介质污染和靶向型病毒积累中演化。
- 系统型病毒从超大网络容量和广谱型病毒积累中演化。
- 多态型病毒不参与普通进化链，保持整合包作者用于特殊黑名单产物的独立规则。

广谱型病毒建议拆成三种变体，它们都属于广谱型等级，但触发条件和潜伏后的感染对象不同：

- 标签广谱型：目标标签下的物品必须在感染网络中全部出现过，才允许生成。生成后潜伏在网络中，后续进入网络且属于该标签、但尚未感染的物品，会按概率被感染。
- 磁盘广谱型：根据某个磁盘的已占用字节逐渐提高生成概率。生成后潜伏在该磁盘或其所在存储上下文中，优先感染后续写入该磁盘的未感染物品。
- 驱动器广谱型：根据某个 ME Drive 中已感染磁盘数量逐渐提高生成概率。生成后潜伏在驱动器层级，优先感染该驱动器内其他磁盘以及后续插入该驱动器的磁盘。

标签广谱型的重点是“同类资源齐全后发生标签级演化”，磁盘广谱型的重点是“单个存储介质逐渐污染”，驱动器广谱型的重点是“多个污染磁盘使整个驱动器变成扩散源”。

推荐函数：

```java
public final class InfectionRisk {
    public static double attemptChance(VirusClass virusClass, InfectionTarget target, NetworkStats network, InfectionConfig config) {
        double pressure = pressure(virusClass, target, network, config);
        if (pressure <= 0.0) {
            return 0.0;
        }

        double normalized = ProbabilityCurves.expSaturation(pressure);
        return clamp(
                config.minAttemptChance() + normalized * (config.maxAttemptChance() - config.minAttemptChance()),
                0.0,
                config.maxAttemptChance()
        );
    }

    private static double pressure(VirusClass virusClass, InfectionTarget target, NetworkStats network, InfectionConfig config) {
        return switch (virusClass) {
            case TARGETED -> network.itemCount(target.item()) / config.targetedItemCountScale();
            case BROAD_SPECTRUM -> broadSpectrumPressure(target, network, config);
            case SYSTEMIC -> systemicPressure(network, config);
            case POLYMORPHIC -> network.blacklistedItemCount() / config.polymorphicBlacklistCountScale();
        };
    }

    private static double broadSpectrumPressure(InfectionTarget target, NetworkStats network, InfectionConfig config) {
        double targetedVirusPressure = network.virusCount(VirusClass.TARGETED) / config.broadTargetedVirusCountScale();

        return switch (target.broadVariant()) {
            case TAG -> tagBroadPressure(target, network, config, targetedVirusPressure);
            case DISK -> diskBroadPressure(target, network, config, targetedVirusPressure);
            case DRIVE -> driveBroadPressure(target, network, config, targetedVirusPressure);
        };
    }

    private static double tagBroadPressure(InfectionTarget target, NetworkStats network, InfectionConfig config, double targetedVirusPressure) {
        if (!network.hasEveryItemInTag(target.tag())) {
            return 0.0;
        }

        double tagPressure = network.tagItemCount(target.tag()) / config.broadTagItemCountScale();

        return tagPressure * config.broadTagWeight()
             + targetedVirusPressure * config.broadTargetedVirusWeight();
    }

    private static double diskBroadPressure(InfectionTarget target, NetworkStats network, InfectionConfig config, double targetedVirusPressure) {
        double diskPressure = network.diskUsedBytes(target.diskId()) / config.broadDiskUsedBytesScale();

        return diskPressure * config.broadDiskWeight()
             + targetedVirusPressure * config.broadTargetedVirusWeight();
    }

    private static double driveBroadPressure(InfectionTarget target, NetworkStats network, InfectionConfig config, double targetedVirusPressure) {
        double infectedDiskPressure = network.infectedDiskCount(target.driveId()) / config.broadDriveInfectedDiskCountScale();

        return infectedDiskPressure * config.broadDriveWeight()
             + targetedVirusPressure * config.broadTargetedVirusWeight();
    }

    private static double systemicPressure(NetworkStats network, InfectionConfig config) {
        double totalBytesPressure = network.totalBytes() / config.systemicTotalBytesScale();
        double broadVirusPressure = network.virusCount(VirusClass.BROAD_SPECTRUM) / config.systemicBroadVirusCountScale();

        return totalBytesPressure * config.systemicTotalBytesWeight()
             + broadVirusPressure * config.systemicBroadVirusWeight();
    }

    public static double successChance(ExposureStats exposure, InfectionConfig config) {
        double cableExposure = cableExposure(exposure, config);
        double machineExposure = machineExposure(exposure, config);
        double weightedExposure = cableExposure + machineExposure;

        if (weightedExposure <= 0.0) {
            return 0.0;
        }

        double breach = ProbabilityCurves.expSaturation(weightedExposure, config.exposureScale());
        return clamp(breach, 0.0, config.maxSuccessChance());
    }

    private static double cableExposure(ExposureStats exposure, InfectionConfig config) {
        return exposure.exposedCableFaces() * config.cableFaceWeight()
             + exposure.wirelessRangeExposures() * config.wirelessRangeWeight();
    }

    private static double machineExposure(ExposureStats exposure, InfectionConfig config) {
        return exposure.machineFaces().stream()
                .filter(MachineFaceExposure::isExposedToAir)
                .filter(face -> face.inputEnabled() || face.outputEnabled())
                .mapToDouble(face -> config.machineFaceWeight(face.machineType()))
                .sum();
    }

    public static InfectionRoll roll(VirusClass virusClass, InfectionTarget target, NetworkStats network, ExposureStats exposure, RandomSource random, InfectionConfig config) {
        double attempt = attemptChance(virusClass, target, network, config);
        if (random.nextDouble() >= attempt) {
            return InfectionRoll.noAttempt(attempt);
        }

        double success = successChance(exposure, config);
        if (random.nextDouble() >= success) {
            return InfectionRoll.failedAttempt(attempt, success);
        }

        return InfectionRoll.success(attempt, success);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
```

参数含义：

- `VirusClass`：病毒类型，包含 `TARGETED`、`BROAD_SPECTRUM`、`SYSTEMIC`、`POLYMORPHIC`。
- `InfectionTarget`：本次病毒尝试使用的目标。靶向型使用物品 ID，广谱型使用标签 ID，多态型使用黑名单目标。
- `InfectionTarget.broadVariant()`：广谱型病毒变体，包含 `TAG`、`DISK`、`DRIVE`。
- `NetworkStats.itemCount(item)`：某个具体物品在 AE 网络中的数量。
- `NetworkStats.tagItemCount(tag)`：某个标签下所有物品在 AE 网络中的总数量。
- `NetworkStats.hasEveryItemInTag(tag)`：目标标签下的所有物品是否都曾出现在当前感染网络中。标签广谱型必须满足该条件，否则不会生成。
- `NetworkStats.diskUsedBytes(diskId)`：指定磁盘的已占用字节。磁盘广谱型根据该值逐渐增加生成概率。
- `NetworkStats.infectedDiskCount(driveId)`：指定驱动器中已被感染的磁盘数量。驱动器广谱型根据该值逐渐增加生成概率。
- `NetworkStats.totalBytes()`：网络总字节数，包含已使用字节和未使用字节，用于系统型病毒。
- `NetworkStats.virusCount(type)`：网络中某类病毒的数量，用于表达病毒进化压力。
- `NetworkStats.blacklistedItemCount()`：病毒生产黑名单物品在网络中的总数量，用于多态型病毒。
- `minAttemptChance`：只要网络中有物品，就存在的最低尝试概率。
- `maxAttemptChance`：单次检测中最高尝试概率，避免超大网络必定每次触发。
- `ExposureStats.exposedCableFaces()`：暴露在空气中的线缆面数量。基础设计中主要统计玻璃线缆，后续也可以让不同线缆类型有不同权重。
- `ExposureStats.machineFaces()`：AE 机器各方向面的暴露状态和输入输出状态。
- `MachineFaceExposure.isExposedToAir()`：该机器面是否暴露在空气中。
- `MachineFaceExposure.inputEnabled()` / `outputEnabled()`：该机器面是否开启主动输入或主动输出。只有暴露在空气中且至少开启一种输入输出能力的机器面，才会计入侵染成功率。
- `wirelessRangeExposures`：无线范围连接器等无线暴露源数量或加权暴露值。
- `exposureScale`：暴露面缩放值。数值越小，少量暴露面也能快速提高成功率。
- `maxSuccessChance`：单次侵染尝试的最高成功率。

推荐默认值：

- `minAttemptChance = 0.0001`
- `maxAttemptChance = 0.05`
- `targetedItemCountScale = 4096`
- `broadTagItemCountScale = 8192`
- `broadDiskUsedBytesScale = 16384`
- `broadDriveInfectedDiskCountScale = 3`
- `broadTagWeight = 0.5`
- `broadDiskWeight = 0.25`
- `broadDriveWeight = 0.25`
- `broadTargetedVirusCountScale = 4`
- `broadTargetedVirusWeight = 0.35`
- `systemicTotalBytesScale = 65536`
- `systemicTotalBytesWeight = 0.7`
- `systemicBroadVirusCountScale = 3`
- `systemicBroadVirusWeight = 0.5`
- `polymorphicBlacklistCountScale = 512`
- `cableFaceWeight = 1.0`
- `machineFaceWeight = 2.0`
- `wirelessRangeWeight = 4.0`
- `exposureScale = 32.0`
- `maxSuccessChance = 0.75`

示例解释：

- 靶向型病毒遇到大量同一种目标物时，尝试概率提高；该物品数量很少时，尝试概率很低。
- 广谱型病毒分为三种变体：标签广谱型要求目标标签下的物品在感染网络中全部出现过，满足后才可能生成；磁盘广谱型随指定磁盘已占用字节增加生成概率；驱动器广谱型随指定驱动器中已感染磁盘数量增加生成概率。三种变体都会受到靶向型病毒数量影响，靶向型病毒越多，越容易进化出广谱型病毒。
- 系统型病毒看网络总字节数和广谱型病毒数量；空的大容量存储阵列也会提高风险，而广谱型病毒越多，越容易进一步演化成系统型病毒。
- 多态型病毒看黑名单物品数量，黑名单资源越多，越容易触发这种特殊病毒。
- 大型主网、压力值很高、但没有暴露面：尝试概率高，但成功率为 0。
- 大型主网、玻璃线缆大量暴露：尝试概率高，成功率也高。
- 机器暴露比普通线缆暴露更危险，但只有该面暴露在空气中并开启主动输入或输出时才计入风险；纯外壳面或关闭输入输出的面不计入机器暴露。
- 无线范围暴露源绕过传统线缆布局，因此默认权重最高。

检测节奏建议：

- 不建议每 tick 检测，可以每 20 秒、60 秒或游戏内固定安全扫描周期检测一次。
- 防火墙在线且供电正常时，可以直接跳过 `roll`，阻止新的病毒侵染。
- 防火墙断电时，可以保留较低保护系数，例如把 `successChance` 乘以 0.25，而不是完全保护。
- 检测失败也可以写入轻量级安全日志，例如“检测到外部扫描，但未突破暴露面”。

### 5.2 ProbabilityCurves

概率曲线建议集中放在 `ProbabilityCurves` 工具类中，避免侵染、纯度、产出倍率等系统各自散落公式。这样后续平衡时只需要调整曲线参数和配置，不需要到处查找 `Math.exp`、`Math.pow` 等调用。

推荐工具类：

```java
public final class ProbabilityCurves {
    private ProbabilityCurves() {
    }

    public static double expSaturation(double value) {
        if (value <= 0.0) {
            return 0.0;
        }

        return 1.0 - Math.exp(-value);
    }

    public static double expSaturation(double value, double scale) {
        if (value <= 0.0 || scale <= 0.0) {
            return 0.0;
        }

        return 1.0 - Math.exp(-value / scale);
    }

    public static double logistic(double value, double midpoint, double steepness) {
        if (steepness <= 0.0) {
            return value >= midpoint ? 1.0 : 0.0;
        }

        return 1.0 / (1.0 + Math.exp(-steepness * (value - midpoint)));
    }

    public static double cappedLinear(double value, double scale) {
        if (value <= 0.0 || scale <= 0.0) {
            return 0.0;
        }

        return clamp(value / scale, 0.0, 1.0);
    }

    public static double power(double value, double scale, double exponent) {
        if (value <= 0.0 || scale <= 0.0 || exponent <= 0.0) {
            return 0.0;
        }

        return Math.pow(value / scale, exponent);
    }

    public static double cappedPower(double value, double scale, double exponent) {
        return clamp(power(value, scale, exponent), 0.0, 1.0);
    }

    public static double lerp(double min, double max, double t) {
        return min + (max - min) * clamp(t, 0.0, 1.0);
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
```

推荐用途：

- `expSaturation(value)`：用于已经归一化过的压力值，例如病毒类型压力。
- `expSaturation(value, scale)`：用于带缩放量的暴露面、字节数、侵染数量等原始数值。
- `logistic(value, midpoint, steepness)`：用于纯度成长、后期培养、接近阈值后快速提升的玩法。
- `cappedLinear(value, scale)`：用于简单、可预测的线性进度，例如小型机器升级进度。
- `cappedPower(value, scale, exponent)`：用于产出倍率、成本倍率等需要前期慢、后期快或前期快、后期慢的曲线。
- `lerp(min, max, t)`：用于把归一化曲线结果映射到真实概率、倍率或时间。

数学含义：

- 指数饱和：`f(x) = 1 - e^(-x / scale)`。适合“风险随规模增加，但逐渐接近上限”的系统。
- Logistic 曲线：`f(x) = 1 / (1 + e^(-k(x - midpoint)))`。适合“低值变化很小，中段快速变化，高值再次放缓”的系统。
- 幂函数：`f(x) = (x / scale)^p`。`p > 1` 时前期更慢、后期更快；`0 < p < 1` 时前期更快、后期更慢。

在 AE2-Virus 中的建议：

- 侵染尝试概率使用 `expSaturation(pressure)`。
- 暴露成功概率使用 `expSaturation(weightedExposure, exposureScale)`。
- 侵染数量到初始纯度可以使用 `expSaturation(infectedAmount, purityScale)`。
- 繁殖机长期生产提升纯度可以使用 `logistic(experience, midpoint, steepness)`。
- 纯度到产出倍率可以使用 `cappedPower(purity, purityScale, exponent)` 后再 `lerp(minMultiplier, maxMultiplier, t)`。
- 纯度到生产时间缩短也可以使用 `lerp(maxTime, minTime, t)`，但不同病毒躯壳应有各自的时间上下限。

示例：

```java
double purityProgress = ProbabilityCurves.expSaturation(infectedAmount, config.purityAmountScale());
double initialPurity = ProbabilityCurves.lerp(config.minInitialPurity(), config.maxInitialPurity(), purityProgress);

double multiplierProgress = ProbabilityCurves.cappedPower(purity, config.purityMultiplierScale(), config.purityMultiplierExponent());
double outputMultiplier = ProbabilityCurves.lerp(config.minOutputMultiplier(), config.maxOutputMultiplier(), multiplierProgress);

double growthProgress = ProbabilityCurves.logistic(virusExperience, config.purityGrowthMidpoint(), config.purityGrowthSteepness());
double trainedPurity = ProbabilityCurves.lerp(currentPurity, config.maxTrainedPurity(), growthProgress);
```

曲线选择原则：

- 如果目标是“越来越高但不爆炸”，使用指数饱和。
- 如果目标是“低级几乎不变，中期开始明显成长”，使用 Logistic。
- 如果目标是“简单可读、容易被玩家理解”，使用线性。
- 如果目标是“用一个指数调节前后期节奏”，使用幂函数。
- 不建议为了这些概率函数引入额外数学库，JDK 的 `Math.exp`、`Math.pow` 已经足够。

### 5.3 玻璃线缆侵染

使用玻璃线缆连接设备时，玻璃线缆每一个暴露在空气中的面都有概率让病毒侵入 AE2 网络。

判定思路：

- 每根玻璃线缆最多有 6 个面。
- 与其他方块连接的面不算暴露面。
- 与空气直接接触的面算暴露面。
- 每个暴露面都独立提供一次入侵概率。
- 暴露面越多，网络被感染的风险越高。

示例：

- 一根四周完全裸露的玻璃线缆风险较高。
- 埋在墙体或方块内部的玻璃线缆风险较低。
- 穿过露天区域的大量玻璃线缆会明显提高网络感染概率。

### 5.4 包覆线缆安全性

包层线缆、智能线缆等有包裹结构的线缆不会被空气直接侵染。它们可以作为玩家提升网络安全等级的重要材料消耗。

安全差异：

- 玻璃线缆：便宜、透明、易感染。
- 包层线缆：基础防护，不会因空气暴露被感染。
- 智能线缆：兼具频道可视化与防护效果。
- 致密线缆：适合作为高安全主干网络。

这样可以让玩家在早期使用玻璃线缆承担一定风险，在中后期逐渐升级为更安全的网络结构。

### 5.5 AE 设备侵染

连接到网络的 AE 设备，如果某一面开启了主动输入或主动输出，并且该面暴露在空气中，则该面有概率成为病毒入侵点。

适用设备示例：

- 输入总线
- 输出总线
- ME 接口
- 样板供应器
- 存储总线
- 输入输出端口
- 其他具有主动输入、主动输出或自动化交互能力的设备

判定规则：

- 只有开启主动输入或主动输出的面会产生入侵风险。
- 没有开启输入输出的面不会被入侵。
- 被方块遮挡、包覆或封闭的工作面风险降低或消失。
- 与其他管道、机器或容器直接连接的面可以视为非空气暴露面。

这鼓励玩家把自动化设备做成封闭机房，而不是把所有输入输出面暴露在空气中。

### 5.6 AE2 附属无线频道设备侵染

部分 AE2 附属模组会加入无线连接频道的设备。这类设备不一定使用实体线缆连接，但在玩法上仍然承担“传递频道、连接网络、扩展网络范围”的职责，因此也应该纳入病毒侵染体系。

无线频道设备可以分为两类：无线连接器类型和范围连接器类型。

### 5.6.1 无线连接器类型

无线连接器类型的设备通过主从结构传递频道。常见形式包括：

- 一主一从：一个主连接器对应一个从连接器。
- 一主多从：一个主连接器向多个从连接器分发频道。
- 多主多从：多个主连接器与多个从连接器组成更复杂的无线频道网络。

这类设备虽然没有实体线缆，但可以理解为在主从设备之间生成了“虚拟线缆”。病毒侵染时，可以按照原有线缆侵染方式处理。

判定规则：

- 主连接器和从连接器之间视为存在一段虚拟线缆。
- 虚拟线缆连接的两端网络可以互相传播病毒。
- 如果主端网络被感染，病毒可以沿虚拟线缆传播到从端网络。
- 如果从端网络被感染，病毒也可以反向传播到主端网络。
- 一主多从结构中，一个感染端可能逐步影响所有已连接从端。
- 多主多从结构中，病毒扩散风险更高，适合被视为高复杂度网络。

为了保持规则统一，虚拟线缆本身可以继承普通线缆的侵染逻辑，但它没有“暴露在空气中的线缆面”。因此它更适合作为病毒扩散通道，而不是主要入侵来源。

推荐设计：

- 无线连接器不会单独因为暴露在空气中而高频感染。
- 当任意一端网络已经感染时，病毒可以通过无线连接器跨网络传播。
- 无线连接器的传播风险与连接数量、频道数量、距离或设备等级相关。
- 防火墙可以阻止病毒通过无线连接器继续传播。
- 日志中应记录病毒通过哪一组无线连接器传播，方便玩家排查。

示例：

- 主网络没有防火墙，从网络通过无线连接器接入。若从网络因为玻璃线缆暴露而感染，病毒可能沿虚拟线缆进入主网络。
- 一个主连接器连接多个远程矿场子网，其中一个矿场子网感染后，病毒可能扩散到其他从端子网。
- 高级无线连接器可以拥有更低传播概率或内置基础隔离能力。

### 5.6.2 范围连接器类型

范围连接器类型的设备可以把频道在一定范围内无线连接到 AE 设备，不需要线缆直接连接。它们相当于把某个区域内的 AE 设备纳入网络，因此本身就是一个高价值的无线暴露点。

这类设备可以直接成为病毒入侵来源。

判定规则：

- 范围连接器方块本体可以被视为一个网络暴露点。
- 范围连接器处于工作状态时，有概率直接让病毒侵入其连接的 AE 网络。
- 覆盖范围越大，连接设备越多，入侵风险越高。
- 范围内存在暴露的主动输入输出设备时，风险进一步提高。
- 范围连接器连接到未受保护网络时，风险明显高于有防火墙保护的网络。

范围连接器的特殊风险在于，它绕过了传统线缆布局。玩家不能只通过包覆线缆来降低风险，还需要考虑无线覆盖范围本身是否安全。

推荐设计：

- 范围连接器应拥有独立的入侵概率。
- 入侵概率可以根据覆盖半径、连接设备数量、工作频道数量计算。
- 范围连接器可以被防火墙完全保护，防火墙在线时不会产生新的病毒入侵。
- 范围连接器可以被扫描日志标记为“无线范围暴露源”。
- 高级范围连接器可以提供更低风险或内置安全模块插槽。

示例：

- 玩家在露天矿场放置范围连接器，让附近多个输入总线无线接入主网。该范围连接器会成为病毒可能直接入侵的入口。
- 玩家在封闭机房内使用范围连接器，并为主网安装防火墙，则该无线网络可以保持安全。
- 一个超大范围连接器覆盖多个自动化区域时，应当比小范围连接器拥有更高风险。

## 6. 防御机制

防火墙方块是 AE2-Virus 的核心防御设备。只要某个 AE2 网络连接了防火墙方块，该网络就不会被新的病毒侵染。

### 6.1 防火墙方块

防火墙方块可以理解为 ME 网络中的安全网关。它会持续保护所连接的网络，使病毒无法从玻璃线缆暴露面或设备暴露面进入网络。

基础效果：

- 阻止新的病毒入侵。
- 提供网络安全状态显示。
- 允许玩家进行主动扫描。
- 允许玩家执行杀毒流程。
- 可作为后续升级系统的核心方块。

防火墙只阻止新的入侵，不一定自动清除已经存在的病毒。若网络在安装防火墙前已经被感染，玩家仍需要主动扫描和杀毒。

### 6.2 防御范围

防火墙保护它所连接的整个 AE2 网络。如果网络被分成多个独立子网，每个子网都需要单独安装防火墙。

建议规则：

- 一个独立网络至少需要一个防火墙。
- 多个防火墙可以提升扫描速度、杀毒成功率或冗余能力。
- 防火墙断电、断线或被拆除时，网络重新暴露在感染风险中。
- 防火墙需要消耗 AE 能量，网络供电不足时防护效果降低或暂停。

### 6.3 安全等级

可以为网络计算一个安全等级，用于显示风险：

- 危险：无防火墙，存在大量暴露玻璃线缆或暴露输入输出设备。
- 脆弱：无防火墙，但暴露面较少。
- 受保护：安装了防火墙，可以阻止新病毒。
- 安全：安装防火墙且无已知病毒。
- 高安全：多个防火墙、全包覆线缆、无暴露主动设备。

## 7. 杀毒机制

防火墙方块可以启动主动扫描和杀毒，风格类似 360 安全卫士：玩家可以手动点击扫描，查看威胁列表，再执行修复或一键杀毒。

### 7.1 扫描

扫描用于发现网络中的病毒、封锁项和损坏项。

扫描结果可以包含：

- 当前网络是否感染。
- 病毒数量或病毒强度。
- 被封锁的物品数量。
- 被封锁的范围类型。
- 已损坏物品数量。
- 已丢失物品记录。
- 预计继续恶化时间。
- 推荐处理方式。

扫描可以消耗：

- AE 能量
- 时间
- 处理核心耐久
- 安全升级卡
- 专用材料，例如“杀毒模块”或“安全数据库”

### 7.2 杀毒

主动杀毒可以清除网络病毒。病毒死亡后，所有由该病毒造成的封锁都会解除。

杀毒结果：

- 病毒被清除。
- 被封锁物品解除封锁。
- 病毒死亡并掉落对应等级的病毒躯壳。
- 损坏物品有概率恢复。
- 未恢复的损坏物品可以保持损坏，等待后续修复流程。
- 已丢失物品无法恢复。

杀毒可以有成功率，也可以根据病毒等级、网络规模、防火墙数量和升级模块决定耗时。

### 7.3 病毒躯壳

病毒被安全助手或防火墙杀毒流程查杀后，不会只从网络中消失，而是留下可回收材料“病毒躯壳”。病毒躯壳代表被清除后的病毒残余代码和结构外壳，是后续制造可控病毒的重要材料。

病毒躯壳不只是材料品质，也决定可控病毒在繁殖机中的生产范围、产量上限和生产时间：

- 靶向型病毒躯壳：仅能生产单一目标物。它的生产范围最窄，但产量可以随纯度和培养程度获得巨大提升，生产时间一般。
- 广谱型病毒躯壳：仅能生产广谱变体允许的产物。标签广谱型面向某个标签内的物品，磁盘广谱型面向被污染磁盘上下文，驱动器广谱型面向被污染驱动器上下文；整体产量低，生产时间长。
- 系统型病毒躯壳：仅能生产该病毒实际影响过的物品集合。每种物品单次产量上限为 1，生产时间极长，适合把一次大规模感染转化为低速、宽范围的回收式生产。
- 多态型病毒躯壳：为整合包作者提供的特殊通道。其他三种病毒可以侵染黑名单物品并产出对应数据流，但不能用这些数据流生产黑名单物品；多态型病毒可以突破该限制，生产这些被标记的物品，产量一般，生产时间一般。

掉落规则建议：

- 杀毒成功时必定掉落至少一个对应等级躯壳。
- 病毒等级越高，掉落数量和完整度越高。
- 安全助手升级可以提高完整躯壳掉落率。
- 自动杀毒可以降低掉落率，避免全自动刷材料过早失控。
- 配置中可以允许整合包作者关闭躯壳掉落，或只允许主动杀毒掉落。

### 7.4 修复

杀毒和修复可以拆成两个步骤：

- 杀毒：清除病毒，解除封锁。
- 修复：尝试恢复损坏物品。

这样可以让玩家在紧急情况下先阻止继续恶化，再花费更多资源恢复损失。

修复结果建议：

- 普通物品恢复概率较高。
- 高价值物品恢复概率较低。
- 损坏时间越久，恢复概率越低。
- 防火墙升级越高，恢复概率越高。
- 可以消耗材料提高恢复率。

## 8. 数据流与病毒制造

数据流是一种液体，代表病毒在封锁物品时从 AE2 网络中提取出来的物品索引、结构信息和自动化痕迹。它不是普通副产物，而是把“被感染过的物品”转化为“可被病毒繁殖机读取的信息模板”的核心媒介。

当病毒感染主网并造成物品封锁时，网络中会逐渐留下数据流。数据流记录本次侵染关联的目标物、侵染数量和纯度，例如铁锭数据流、钻石数据流或某个具体物品的数据流。玩家后续可以收集这些数据流，用于制造能够生产对应物品的病毒。

### 8.1 数据流的来源

数据流由病毒封锁过程产生，而不是凭空合成。

推荐来源：

- 物品被病毒封锁时，按封锁数量缓慢生成数据流。
- 物品从封锁进入损坏阶段时，生成额外数据流。
- 杀毒时可以回收一部分该病毒已产生的数据流。
- 侵染数量越高，数据流初始纯度越高，后续制造效率越好。
- 防火墙日志可以显示本次感染产生了哪类数据流。

为了避免鼓励玩家故意摧毁主网，数据流产量和初始纯度都应有上限，并且早期靶向型病毒只产生少量低纯度数据流。玩家可以把它看作一次事故后的研究样本，而不是主要资源来源。

### 8.2 数据流的物品信息

数据流需要区分它记录的是哪一种目标。这里可以借鉴资源蜜蜂的可配置蜜蜂蛋和可配置蜂巢产物思路：同一个基础物品或液体，通过附加数据区分具体类型。

资源蜜蜂当前实现中，可配置刷怪蛋仍通过 `ENTITY_DATA` / NBT 写入蜜蜂类型，但可配置蜂巢产物等系统已经使用 Data Component，例如 `productivebees:bee_type`。AE2-Virus 应采用更现代、更便携的方案：

- 数据流本体是同一种注册流体，例如 `ae2virus:data_stream`。
- 数据流使用专用 Data Component 或等价的类型载体记录目标，例如 `ae2virus:data_stream_payload`。
- 载体中保存稳定的 `ResourceLocation`，而不是自由格式 NBT 字符串。
- 靶向型目标保存被侵染的物品 ID，例如 `minecraft:iron_ingot`。
- 广谱型目标保存被侵染物品所属的标签 ID，例如 `#c:ingots/iron`。
- 系统型目标保存本次病毒实际影响过的物品集合，集合中的每一项都是具体物品 ID。
- 多态型目标保存被整合包标记为特殊生产权限的物品 ID。
- NBT 只作为兼容旧存档、指令调试或外部模组交互的迁移层，不作为主要判定依据。

推荐数据结构只保留三个核心字段：

- `target`：目标物，可以是具体物品、标签或系统型物品集合。
- `infected_amount`：侵染数量，即本次数据流来自多少个被封锁或被损坏的目标物。
- `purity`：纯度，由侵染数量换算得到，并影响繁殖机产出倍率。

推荐关系：

- 侵染数量越高，初始纯度越高。
- 纯度越高，病毒繁殖机产出倍率越高。
- 纯度应有上限，避免通过一次大规模事故直接获得无限倍率。
- 多份同目标数据流合并时，可以累加侵染数量，并按规则重新计算纯度。
- 不同目标的数据流不能直接合并，必须保持目标一致。
- 防火墙日志可以同时显示目标物、侵染数量和纯度，便于玩家判断回收价值。

这样做的好处：

- 一个数据流流体可以承载大量目标物，不需要为每个物品注册独立流体。
- 配方、JEI/EMI 显示、网络同步和物品比较可以围绕 Data Component 做精确匹配。
- 数据包可以添加新的数据流映射，整合包不需要写 Java 代码。
- 后续如果需要把数据流装入桶、罐、胶囊或 AE 流体存储，也能保持同一套类型信息。
- 指令、战利品表、配方和机器输入可以用组件谓词匹配特定数据流，比散落 NBT 字符串更稳定。

### 8.3 数据流容器

因为流体本身在很多机器和管道中不总是容易保留复杂数据，建议提供“数据流单元”作为便携容器。

可选形式：

- 数据流桶：适合玩家手动搬运，容量大但堆叠性差。
- 数据流胶囊：适合配方和机器输入，保留完整 Data Component。
- 数据流存储单元：适合接入 AE 流体网络，允许批量管理。
- 数据流样本：小容量物品，用于 JEI/EMI 展示和机器配方。

如果管道或储罐不能保留 Data Component，机器内部应优先使用数据流胶囊或数据流样本作为精确输入；普通流体管道只适合搬运未指定或已标准化的数据流。

### 8.4 病毒核心制造机

病毒核心是制造可控病毒的基础胚体。它不包含具体产物信息，只提供病毒运行所需的结构、能量通道和等级上限。

病毒核心可以通过“病毒核心制造机”生产。

输入建议：

- AE 处理器或计算处理器。
- 存储元件、逻辑处理器、工程处理器等 AE 材料。
- 安全模块或防火墙材料。
- AE 能量或 FE。
- 可选催化材料，例如损坏数据包、污染物品索引或安全数据库。

输出建议：

- 靶向病毒核心。
- 广谱病毒核心。
- 系统病毒核心。
- 多态病毒核心。

核心等级决定后续病毒的等级上限、可接受的数据流纯度、繁殖机效率上限和失控风险。

### 8.5 病毒组装机

病毒组装机用于把病毒核心、数据流和病毒躯壳组装成可控病毒。

基础配方结构：

1. 输入病毒核心。
2. 输入带有物品信息的数据流。
3. 输入对应等级或更高等级的病毒躯壳。
4. 消耗能量与时间。
5. 输出携带目标数据的病毒。

组装规则：

- 数据流决定病毒后续可以读取的目标。
- 病毒躯壳决定病毒的生产范围、产量上限、生产时间和是否允许生产黑名单物品。
- 病毒核心决定病毒可运行的平台和效率上限。
- 若躯壳等级低于数据流等级，组装失败或产出不稳定病毒。
- 高纯度数据流可以提高最终病毒的产出效率。
- 多态型躯壳可以让病毒获得特殊生产权限，例如生产被整合包列入病毒生产黑名单的物品。

可控病毒不应默认继续感染玩家主网。它应被视为经过封装的生产单元，只能在专用繁殖机中运行。只有在配置允许或机器损坏、过载、断电等特殊情况下，它才可能重新变成网络威胁。

### 8.6 病毒繁殖机

病毒繁殖机是使用可控病毒生产物品的机器。它通过消耗电力和任意物品，让病毒读取自身携带的数据流信息，并把输入物质重写为对应物品。

基础工作流程：

1. 放入可控病毒。
2. 输入电力。
3. 输入任意可消耗物品。
4. 机器读取病毒携带的数据流目标。
5. 消耗输入物品和能量。
6. 输出数据流对应的物品。

躯壳生产规则：

- 靶向型病毒：只能绑定一个具体目标物。纯度提升后可以获得很高产出倍率，适合专门培养单一资源。
- 广谱型病毒：根据变体绑定标签目标、磁盘目标或驱动器目标。标签广谱型从标签产物中选择或按配方指定产出；磁盘广谱型从该磁盘被感染或后续写入感染的物品中产出；驱动器广谱型从该驱动器污染范围内的物品中产出。倍率较低，生产时间较长。
- 系统型病毒：绑定该病毒实际影响过的物品集合。每次可以从集合中选择产物，但每种物品单次产量上限为 1，生产时间极长。
- 多态型病毒：绑定黑名单目标物或特殊数据流。它可以生产普通病毒不能生产的黑名单物品，倍率和生产时间保持中等。

输入物品规则：

- 默认可消耗任意物品。
- 与 AE 的物质聚合器共享黑名单和白名单逻辑。
- 机器自身额外提供独立黑名单和白名单配置。
- 独立配置可以选择继承、叠加或覆盖物质聚合器规则。
- 被禁止复制或高风险的物品应默认加入黑名单。

生产权限规则：

- 侵染权限和生产权限分开计算。
- 靶向型、广谱型和系统型病毒可以侵染黑名单物品，并正常产出对应数据流。
- 靶向型、广谱型和系统型病毒不能用黑名单数据流生产对应物品。
- 多态型病毒可以生产被病毒生产黑名单限制的物品。
- 多态型生产权限应由整合包作者通过数据包或配置显式授予，默认不应覆盖所有黑名单。

推荐配置模式：

- 继承模式：完全使用 AE 物质聚合器的黑白名单。
- 叠加模式：先通过物质聚合器规则，再通过本机额外规则。
- 覆盖模式：只使用本机规则，供整合包作者做特殊玩法。

繁殖效率可以由以下因素决定：

- 病毒等级。
- 数据流纯度。
- 病毒核心等级。
- 病毒躯壳品质。
- 输入物品的质量、稀有度或配置权重。
- 电力消耗和机器升级。

纯度成长规则：

- 病毒繁殖机每次成功生产目标物时，可以为该病毒或其内部数据流积累少量熟练度。
- 熟练度达到阈值后，数据流纯度小幅提升。
- 纯度提升会提高后续产出倍率，但同时可以提高能耗或维护压力。
- 低纯度数据流适合启动生产，高纯度数据流代表病毒已经被长期训练。
- 纯度提升应受机器等级、病毒核心等级和配置上限限制。
- 拆出病毒或转移数据流时应保留纯度，形成可培养、可交易的病毒资产。

平衡建议：

- 任意物品不应等价于免费物质，建议给不同物品设置物质值或最低消耗数量。
- 默认规则应禁止创造性物品、容器型物品、带复杂组件且不可安全复制的物品。
- 产出高价值物品时，电力消耗和输入物质量应显著提高。
- 病毒可以有耐久、活性或稳定度，长期运行后需要维护。
- 低稳定度病毒可能降低效率、产生废液、损坏自身，或触发安全警报。

## 9. 玩家流程

### 9.1 早期

玩家刚开始使用 AE2 网络，主要使用玻璃线缆。此时病毒风险较低但已经存在。

体验重点：

- 偶尔出现轻微封锁。
- 玩家第一次意识到网络安全问题。
- 防火墙成为一个明确的中期目标。
- 首次杀毒后获得靶向型病毒躯壳和少量低纯度数据流样本，提示病毒系统也可以被研究利用。

### 9.2 中期

玩家开始扩大网络，使用自动化输入输出设备，病毒风险明显提升。

体验重点：

- 大规模玻璃线缆暴露带来感染风险。
- 自动化设备暴露面成为新的安全问题。
- 玩家需要规划机房、包覆线缆和防火墙。
- 玩家开始制造病毒核心，并尝试用数据流、躯壳和核心组装可控病毒。

### 9.3 后期

玩家拥有大型网络、复杂自动化和大量存储元件。病毒可以成为维护大型系统的一部分。

体验重点：

- 系统型病毒可能影响整个网络。
- 多防火墙和安全升级成为大型基地标配。
- 玩家可以主动扫描、定期维护、隔离子网。
- 安全设计成为 AE2 网络工程的一部分。
- 病毒繁殖机成为高成本、高可控的物质转化系统，与 AE 物质聚合器规则保持联动。

## 10. 平衡建议

为了避免病毒系统变成纯粹折磨，建议遵循以下原则：

- 病毒入侵应有可理解来源，而不是无条件随机发生。
- 低等级病毒更多制造麻烦，不应频繁造成永久损失。
- 物品丢失前必须有足够时间窗口和明显预警。
- 防火墙应当可靠，安装后可以稳定阻止新的入侵。
- 杀毒成本应与病毒严重程度和网络规模相关。
- 高价值物品可以更容易成为目标，但丢失概率应谨慎控制。
- 默认配置应偏温和，允许整合包作者提高难度。

推荐默认节奏：

- 入侵概率低，但长期暴露会逐渐累积风险。
- 封锁发生较快，让玩家注意到异常。
- 损坏发生较慢，给玩家处理时间。
- 丢失发生最慢，并且只在玩家长期忽视问题时出现。
- 数据流和病毒躯壳应是事故回收材料，不应让玩家通过反复感染主网获得无成本收益。
- 病毒繁殖机应比普通自动化更昂贵、更耗电，但提供独特的“从感染信息转化物品”的玩法价值。

## 11. 可扩展内容

后续可以围绕网络安全继续扩展更多玩法。

### 11.1 病毒类型

- 锁仓病毒：专门封锁存储元件。
- 标签病毒：按照物品标签扩散。
- 合成病毒：干扰自动合成任务。
- 物流病毒：干扰输入输出总线。
- 索引病毒：让终端显示错误数量或虚假物品。
- 勒索病毒：封锁大量物品，要求玩家消耗特定资源解锁。
- 多态病毒：杀毒失败后改变攻击方式。

### 11.2 防火墙升级

- 扫描加速卡：缩短扫描时间。
- 杀毒增强卡：提高杀毒成功率。
- 修复增强卡：提高损坏物品恢复概率。
- 日志模块：记录病毒入侵来源。
- 隔离模块：把感染区域限制在局部网络。
- 自动扫描模块：定期扫描网络。
- 自动杀毒模块：发现病毒后自动处理。
- 样本回收模块：提高数据流和病毒躯壳回收率。

### 11.3 网络隔离

可以引入隔离设备，让玩家把大型 AE2 网络分割成安全区域。

可能玩法：

- 感染只在当前子网扩散。
- 防火墙可以阻断病毒跨网传播。
- 隔离门可以临时切断高风险设备。
- 玩家可以建立“危险自动化区”和“安全主存储区”。

### 11.4 日志与追踪

防火墙可以记录病毒入侵点，帮助玩家修复网络设计。

日志示例：

- 发现病毒：玻璃线缆暴露面
- 位置：x, y, z
- 风险等级：中
- 感染时间：游戏日 128
- 建议：替换为包层线缆或遮挡暴露面
- 数据流：目标物 `minecraft:iron_ingot`，侵染数量 320，纯度 42%

### 11.5 数据流定义

可以允许数据包添加或覆盖数据流定义，让整合包作者控制哪些物品可以被病毒繁殖机生产，以及生产成本如何计算。

示例字段：

- `target`：目标物品。
- `output`：繁殖机产出物品。
- `required_virus_class`：最低病毒等级，例如 `targeted`、`broad_spectrum`、`systemic`、`polymorphic`。
- `energy_cost`：每次生产消耗能量。
- `matter_cost`：输入物质消耗权重。
- `min_purity`：制造或繁殖时需要的最低纯度。
- `blacklist_by_default`：是否默认禁用，需要整合包显式开启。

## 12. 侵染性能与缓存策略

侵染系统必须避免每 tick 扫描整个 AE 网络。推荐实现方式是“网络级缓存 + 事件驱动 dirty 标记 + 低频定时判定 + tick budget”。侵染概率函数只读取缓存快照，不在判定时遍历所有存储、线缆和机器。

### 12.1 网络风险缓存

每个 AE 网络维护一个风险缓存，用于保存侵染概率所需的统计量。

推荐结构：

```java
public final class VirusNetworkRiskCache {
    private final Object2LongMap<Item> itemCounts = new Object2LongOpenHashMap<>();
    private final Object2LongMap<TagKey<Item>> tagItemCounts = new Object2LongOpenHashMap<>();

    private long diskUsedBytes;
    private long driveUsedBytes;
    private long totalBytes;
    private long blacklistedItemCount;

    private int targetedVirusCount;
    private int broadSpectrumVirusCount;

    private int exposedCableFaces;
    private int wirelessRangeExposures;
    private double machineExposureWeight;

    private boolean storageDirty;
    private boolean exposureDirty;
    private boolean virusDirty;
    private boolean candidatesDirty;
}
```

缓存内容按职责拆分：

- 存储缓存：物品数量、标签物品数量、磁盘占用字节、驱动器占用字节、网络总字节、黑名单物品数量。
- 暴露缓存：线缆暴露面、无线范围暴露源、机器暴露权重。
- 病毒缓存：靶向型病毒数量、广谱型病毒数量，以及后续需要的其他病毒计数。
- 候选缓存：可被当前病毒类型选择的目标物、标签或物品集合。

侵染判定时只读取这些缓存字段，不直接扫描 AE 存储内容或世界方块。

### 12.2 存储统计更新

存储统计应优先使用增量更新。

物品进入或离开网络时：

1. 更新 `itemCounts[item]`。
2. 查询该物品关联的标签。
3. 对每个相关标签更新 `tagItemCounts[tag]`。
4. 如果该物品命中病毒生产黑名单，更新 `blacklistedItemCount`。
5. 更新磁盘、驱动器和网络字节统计。
6. 标记 `storageDirty` 或直接更新缓存版本号。

标签查询不应在每次侵染判定时执行。可以在资源重载后构建 `item -> tags` 的反向索引，物品变化时直接查索引。

黑名单也应预编译：

- 具体物品黑名单使用 `Set<Item>`。
- 标签黑名单使用反向索引在物品变化时处理。
- 复杂组件物品默认不参与普通增量统计，必要时只记录为高风险候选。

### 12.3 暴露面局部更新

暴露面检测不应全网扫描。线缆、机器或邻居方块变化时，只更新受影响方块周围的局部状态。

线缆暴露更新：

- 线缆方块放置或移除时，检查自身 6 个面。
- 邻居方块变化时，只检查与该邻居相邻的那一面。
- 旧状态和新状态做差值，增量更新 `exposedCableFaces`。
- 包层线缆、智能线缆、致密线缆默认不计入普通线缆暴露，除非配置允许。

机器暴露更新：

- 机器方块放置、移除、旋转或邻居变化时，检查该机器 6 个面。
- 机器输入输出配置变化时，只重算该机器自身 6 个面。
- 面必须同时满足“暴露在空气中”和“开启主动输入或输出”，才计入机器暴露。
- 不同机器类型可以返回不同权重，例如输入总线、输出总线、接口、样板供应器可以有不同 `machineFaceWeight`。

无线暴露更新：

- 范围连接器启用、关闭、改变范围或改变频道时更新 `wirelessRangeExposures`。
- 无线连接器更适合作为传播通道，不建议频繁参与新入侵暴露统计。

### 12.4 低频检测与 Tick Budget

侵染检测不应每 tick 执行。推荐每个 AE 网络按安全周期检测一次。

推荐节奏：

- 默认每 20 到 60 秒检测一次。
- 每个网络拥有独立 `nextRiskCheckTime`。
- 初始化或网络结构变化后加入随机抖动，避免所有网络同一 tick 同时检测。
- 服务器每 tick 最多处理固定数量的网络风险检查。
- 超出预算的网络进入队列，延迟到后续 tick。

示例流程：

```java
public void tickRiskScheduler(ServerLevel level) {
    int budget = config.maxRiskChecksPerTick();

    while (budget > 0 && !riskQueue.isEmpty()) {
        GridRef grid = riskQueue.poll();
        VirusNetworkRiskCache cache = cacheManager.get(grid);

        cache.refreshIfDirty(config.maxCacheRefreshWorkPerTick());
        if (!cache.ready()) {
            riskQueue.offer(grid);
            break;
        }

        runInfectionRoll(grid, cache.snapshot());
        scheduleNextCheck(grid, level.getGameTime());
        budget--;
    }
}
```

### 12.5 Dirty 标记策略

不同变化只标记对应缓存 dirty，避免一次小变化触发全量重算。

推荐规则：

- 物品数量变化：标记 `storageDirty` 和 `candidatesDirty`。
- 存储元件插入、移除或格式变化：标记 `storageDirty`。
- ME Drive 放置、移除或磁盘变化：标记 `storageDirty`。
- 线缆、机器或邻居方块变化：标记 `exposureDirty`。
- 机器输入输出配置变化：标记 `exposureDirty`。
- 病毒新增、清除或等级变化：标记 `virusDirty` 和 `candidatesDirty`。
- 黑名单配置或数据包重载：标记所有网络的 `storageDirty` 和 `candidatesDirty`。

缓存刷新可以分批进行。大型网络拆分或合并时，如果无法可靠增量迁移，允许标记为需要重建，但重建应分多 tick 完成。

### 12.6 候选池优化

侵染成功后，不应再全网搜索目标。应提前维护候选池。

候选池建议：

- 靶向型：缓存数量大于 0 的高数量物品。
- 广谱型：缓存标签候选和标签下可影响物品。
- 系统型：缓存网络存储快照中的物品集合。
- 多态型：缓存命中病毒生产黑名单的物品集合。

候选池只在 `candidatesDirty` 时重建。对于大型网络，候选池可以分批重建，期间继续使用旧快照，直到新快照完成。

### 12.7 线程与安全

AE 网络和世界方块状态应在主线程读取和应用。若需要异步优化，只允许异步处理不可变快照。

安全规则：

- 主线程生成 `RiskSnapshot`。
- 异步线程只能计算概率、排序候选或预构建权重表。
- 异步结果回到主线程后，必须校验网络版本号。
- 版本号不一致时丢弃结果，等待下一次快照。
- 不在异步线程直接访问方块实体、Grid 节点或 AE 存储 API。

### 12.8 性能红线

实现时应避免以下行为：

- 每 tick 遍历所有 AE 网络。
- 每次侵染判定遍历所有存储内容。
- 每次侵染判定遍历所有线缆和机器。
- 每次候选选择时扫描整个网络。
- 每次物品变化都同步重算所有标签和候选池。
- 在异步线程读写世界方块、方块实体或 AE Grid。

### 12.9 推荐性能库

推荐优先使用 Minecraft 和 NeoForge 生态中已经常见的轻量级性能库，避免为了侵染系统引入过重的运行时依赖。

首选库是 fastutil。Minecraft 本身大量使用 fastutil，适合本系统的计数缓存、集合缓存和候选池。

推荐用途：

- `Object2LongOpenHashMap<Item>`：缓存具体物品数量。
- `Object2LongOpenHashMap<TagKey<Item>>`：缓存标签物品数量。
- `Object2IntOpenHashMap<ResourceLocation>`：缓存病毒类型、目标 ID 或数据流定义计数。
- `ObjectOpenHashSet<Item>`：缓存病毒生产黑名单物品。
- `ObjectArrayList<Item>`：缓存候选物品池。
- `Long2ObjectOpenHashMap<VirusNetworkRiskCache>`：如果网络可以映射为稳定 long id，可用于网络缓存索引。

不建议起步使用 `HashMap<Item, Long>` 或 `Map<TagKey<Item>, Long>` 做高频计数，因为 `Long` 装箱会在大型网络或频繁变动时产生额外分配。

可选库是 Caffeine。它适合自动过期缓存和最大容量缓存，例如：

- `item -> tags` 反向查询缓存。
- 临时 `RiskSnapshot` 缓存。
- 大型候选池的短期缓存。

不过，AE 网络风险缓存通常可以跟随 Grid 生命周期手动创建和销毁，生命周期比较明确。因此 Caffeine 不应作为第一阶段必需依赖，只有在候选池、标签查询或快照缓存出现复杂过期需求时再考虑引入。

Agrona 和 JCTools 暂不推荐作为初始依赖。它们适合更极限的低延迟队列、ring buffer 或并发结构，但侵染系统应设计为低频检测和主线程应用结果，通常用 `ArrayDeque`、fastutil 集合和不可变快照就足够。

推荐依赖策略：

- 第一阶段：只使用 fastutil 和 JDK 集合。
- 第二阶段：如果标签反向索引或候选池缓存需要自动过期，再考虑 Caffeine。
- 不要为了普通风险检测引入 Agrona 或 JCTools。

性能关键不在库本身，而在增量更新、低频判定、局部暴露刷新和候选池缓存。库只能减少常数开销，不能替代正确的数据流设计。

## 13. 配置项建议

为了兼容不同整合包难度，建议提供配置项：

- 是否启用病毒系统。
- 靶向型病毒目标物数量缩放值。
- 广谱型病毒标签物品数量缩放值。
- 广谱型病毒磁盘占用字节缩放值。
- 广谱型病毒驱动器已感染磁盘数量缩放值。
- 广谱型病毒靶向型病毒数量缩放值。
- 广谱型病毒标签、磁盘、驱动器已感染磁盘数量、靶向型病毒数量四项权重。
- 系统型病毒网络总字节缩放值。
- 系统型病毒广谱型病毒数量缩放值。
- 系统型病毒网络总字节和广谱型病毒数量两项权重。
- 多态型病毒黑名单物品数量缩放值。
- 最低侵染尝试概率。
- 最高侵染尝试概率。
- 线缆暴露面权重。
- 机器暴露面权重。
- 机器面是否必须开启输入或输出才计入暴露风险。
- 无线范围暴露源权重。
- 暴露面成功率缩放值。
- 单次侵染尝试最高成功率。
- 无线连接器虚拟线缆传播概率。
- 范围连接器基础入侵概率。
- 范围连接器覆盖半径对入侵概率的影响。
- 范围连接器连接设备数量对入侵概率的影响。
- 封锁到损坏的时间。
- 损坏到丢失的时间。
- 丢失概率。
- 物品封锁产生数据流的基础倍率。
- 损坏阶段产生额外数据流的倍率。
- 杀毒时数据流回收比例。
- 侵染数量到纯度的换算曲线。
- 侵染数量到纯度的指数饱和缩放值。
- 数据流初始纯度上限。
- 繁殖机生产时纯度提升速度。
- 纯度成长 Logistic 中点和陡峭度。
- 繁殖机允许达到的纯度上限。
- 纯度到产出倍率的幂函数缩放值和指数。
- 产出倍率最小值和最大值。
- 纯度到生产时间缩短的最小时间和最大时间。
- 是否允许自动杀毒掉落病毒躯壳。
- 各等级病毒躯壳掉落数量和概率。
- 数据流是否必须使用 Data Component 区分目标。
- 是否允许 NBT 数据流作为旧存档兼容输入。
- 病毒核心制造机能量消耗。
- 病毒组装机能量消耗和失败概率。
- 病毒繁殖机基础能耗。
- 病毒繁殖机是否继承 AE 物质聚合器黑白名单。
- 病毒繁殖机独立黑名单和白名单。
- 病毒繁殖机黑白名单模式：继承、叠加或覆盖。
- 病毒繁殖机是否允许复制带复杂组件的物品。
- 病毒稳定度是否随运行下降。
- 损坏物品恢复概率。
- 防火墙是否需要能量。
- 防火墙扫描速度。
- 防火墙杀毒成功率。
- 风险检测基础间隔。
- 风险检测随机抖动。
- 每 tick 最大网络风险检测数量。
- 每 tick 最大缓存刷新工作量。
- 是否启用候选池缓存。
- 是否允许异步处理风险快照。
- 是否允许病毒影响自动合成。
- 是否允许病毒造成永久物品丢失。
- 是否显示详细入侵日志。

## 14. 一句话体验定位

AE2-Virus 让 AE2 网络从“只要能连上就能运行”的存储系统，变成一个需要考虑暴露面、防护、隔离、扫描、恢复，并能把网络事故转化为可控病毒生产链的网络工程系统。
