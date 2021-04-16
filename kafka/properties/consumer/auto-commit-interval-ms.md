> Based on that description alone, users new to Kafka might assume that offsets are committed in these intervals by a background thread. In reality, offsets are committed during the consumer’s poll method execution, and the `auto.commit.interval.ms` only defines the minimum delay between commits. Only offsets of records returned in previous poll calls are committed. Since processing happens between poll calls, offsets of unprocessed records will never be committed. This guarantees at-least-once [delivery semantics](https://kafka.apache.org/documentation/#semantics).

光根据`auto.commit-interval.ms`会认为kafka会启动一条background线程去进行提交，实际上`committed`操作会再每次poll方法被调用的时候执行， `auto.commit-interval.ms`参数只用于定义两次commit之间的延迟。只会commit上一次poll的记录的offset。未处理的记录不会被提交。



