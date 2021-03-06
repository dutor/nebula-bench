# nebula-bench

`Nebula-Bench` 用于测试 nebula-graph 的基线性能数据，使用 LDBC v0.3.3 的标准数据集。

当前只适用于 nebula graph v2.0 以上版本。

主要功能:

* 生产 LDBC 数据集并导入 nebula graph。
* nebula-graph benchmark。 (未完成)

## 使用说明

### 安装准备

```bash
sudo yum install -y git \
                    make \
                    file \
                    libev \
                    libev-devel \
                    gcc \
                    wget \
                    python3 \
                    python3-devel \
                    java-1.8.0-openjdk \
                    maven 


```

```bash
git clone https://github.com/vesoft-inc/nebula-bench.git 
cd nebula-bench
pip3 install --user -r requirements.txt
python3 run.py --help
```

### 生成 LDBC 数据

```bash
python3 run.py data 
```

会自动下载 Hadoop，然后使用 [ldbc_snb_datagen](https://github.com/ldbc/ldbc_snb_datagen_spark) 生成数据。
为了方便 importer 导入，将生成后的文件拆分了一个带头的 header 文件，再去掉原有文件第一行。
默认生成的文件在 `${PWD}/target/data/test_data/`。

更多命令

```bash
# generate sf10 ldbc data
python3 run.py data  -s 10

# change hadoop options
export HADOOP_CLIENT_OPTS="-Xmx8G"
python3 run.py data -s 100
# only generate, do not split the data
python3 run.py data -og

# split data, no need generate again.
python3 run.py data -os
```

### 导入数据到 nebula-graph

```bash
python3 run.py nebula importer
```

会根据 header 文件，自动生成 importer 的配置文件，然后运行 importer 导入。
需要注意，默认的 `nebula-importer` 是 linux 下编译的，如果需要在 Mac OS 上使用，请自行编译一个新的 `nebula-importer`。

```bash
# after prepare the data, you could import the data to any nebula graph as you want.
# space is mytest, graph address is 127.0.0.1:9669
python3 run.py nebula importer -s mytest -a 127.0.0.1:9669

# or using dotenv
cp env .env
# vi .env
python3 run.py nebula importer

# dry run, just create the import config file, and you can modify any configuration。
# by default, PARTITION_NUM is 24, REPLICA_FACTOR is 3.
python3 run.py nebula importer --dry-run
```

### nebula benchmark

使用带有 [xk6-nebula](https://github.com/HarrisChu/xk6-nebula) 插件的 [K6](https://github.com/k6io/k6) 来进行压测。
需要注意，默认的 `k6` 是 linux 下编译的，如果需要在 Mac OS 上使用，请自行下载对应的二进制文件。[xk6-nebula](https://github.com/HarrisChu/xk6-nebula/tags)

自动化的场景，在 `nebula_bench/scenarios/` 中。

```bash
# show help
python3 run.py stress run --help

# run all scenarios with 100 virtual users, every scenario lasts 60 seconds.
python3 run.py stress run 

# run all scenarios with 10 virtual users, every scenario lasts 3 seconds.
python3 run.py stress run -vu 10 -d 3

# run go.Go1Step scenarios with 10 virtual users, every scenario lasts 3 seconds.
python3 run.py stress run -vu 10 -d 3 -s go.Go1Step
```

k6 config file, summary result and outputs are in `output` folder. e.g.

```bash
# you should install jq to parse json.
# how many checks
jq .metrics.checks output/result_Go1Step.json

# summary latency
jq .metrics.latency output/result_Go1Step.json

# summary error message 
awk -F ',' 'NR>1{print $NF}' output/output_Go1Step.csv |sort|uniq -c
```

如果使用 Jmeter，暂时没有自动化操作，可以通过手动调整 Jmeter 来测试，具体参考 [jmx](ldbc/jmx/go_step.jmx) 和 [java](util/LdbcGoStep/src/main/java/vesoft/LdbcGoStep.java)。

## 更多

* 生成的数据文件，如果是 `aaa_xxYY_bbb` 格式，比如 `comment_hasTag_tag`，会认为是一个边类型，然后边的格式是 `XX_YY`。和 ldbc 保持一致 [ldbc_snb_interactive](https://github.com/ldbc/ldbc_snb_interactive/blob/main/cypher/queries/interactive-complex-1.cypher)
* 否则是一个 Tag 类型。
* 不同的 Tag，有可能 Vertex ID 是一样的，比如 Forum 和 Post。暂时没有特殊处理。
