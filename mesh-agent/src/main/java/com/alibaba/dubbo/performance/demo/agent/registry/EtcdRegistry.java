package com.alibaba.dubbo.performance.demo.agent.registry;

import com.alibaba.dubbo.performance.demo.agent.utils.EnumKey;
import com.coreos.jetcd.Client;
import com.coreos.jetcd.KV;
import com.coreos.jetcd.Lease;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.kv.GetResponse;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.PutOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class EtcdRegistry implements IRegistry {
    private Logger logger = LoggerFactory.getLogger(EtcdRegistry.class);
    // 该EtcdRegistry没有使用etcd的Watch机制来监听etcd的事件
    // 添加watch，在本地内存缓存地址列表，可减少网络调用的次数
    // 使用的是简单的随机负载均衡，如果provider性能不一致，随机策略会影响性能

    private Lease lease;
    private KV kv;
    private long leaseId;
    private ExecutorService monitor=null;

    public EtcdRegistry(String registryAddress) {
        System.out.println(registryAddress);
        Client client = Client.builder().endpoints(registryAddress).build();
        this.lease   = client.getLeaseClient();
        this.kv      = client.getKVClient();
        try {
            this.leaseId = lease.grant(30).get().getID();
        } catch (Exception e) {
            e.printStackTrace();
        }

        String type=System.getProperty("type");
        if ("provider".equals(type)){
            // 如果是provider，去etcd注册服务
           // keepAlive();
            try {
                int port = Integer.valueOf(System.getProperty("server.port"));
                register("com.alibaba.dubbo.performance.demo.provider.IHelloService",port);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 向ETCD中注册服务
    public void register(String serviceName,int port) throws Exception {
        // 服务注册的key为-->/serviceName/level    /com.some.package.IHelloService/small
        String strKey = MessageFormat.format("/{0}/{1}",serviceName,System.getProperty("level"));
        ByteSequence key = ByteSequence.fromString(strKey);
        // 服务注册的val为-->ip:port   /127.0.0.1:30000
        String strVal= MessageFormat.format("{0}:{1}",IpHelper.getHostIp(),String.valueOf(port));
        ByteSequence val = ByteSequence.fromString(strVal);
        kv.put(key,val, PutOption.newBuilder().withLeaseId(leaseId).build()).get();
        logger.info("Register a new service at:" + strKey);
    }

    // 发送心跳到ETCD,表明该host是活着的
    public void keepAlive(){
        monitor=Executors.newSingleThreadExecutor();
        monitor.submit(
                () -> {
                    System.err.println("检测!");
                    try {
                        Lease.KeepAliveListener listener = lease.keepAlive(leaseId);

                        logger.info("KeepAlive lease:" + leaseId + "; Hex format:" + Long.toHexString(leaseId));
                        if(findLsx()){
                            listener.close();
                            System.out.println("退出心跳!");
                        }else{
                            listener.listen();
                            System.out.println("还没退出心跳!");
                        }
                        monitor.shutdown();
                    } catch (Exception e) { e.printStackTrace(); }
                }
        );
    }

    public Map<EnumKey,Endpoint> find(String serviceName) throws Exception {

        String strKey = MessageFormat.format("/{0}",serviceName);
        ByteSequence key  = ByteSequence.fromString(strKey);
        GetResponse response = kv.get(key, GetOption.newBuilder().withPrefix(key).build()).get();

        //List<Endpoint> endpoints = new ArrayList<>();

        Map<EnumKey,Endpoint> level2EndPoint=new EnumMap<EnumKey, Endpoint>(EnumKey.class);

        for (com.coreos.jetcd.data.KeyValue kv : response.getKvs()){
            String k = kv.getKey().toStringUtf8();
            int index = k.lastIndexOf("/");
            String levelStr = k.substring(index + 1,k.length());

            String v= kv.getValue().toStringUtf8();
            String host = v.split(":")[0];
            int port = Integer.valueOf(v.split(":")[1]);
            if("small".equals(levelStr)){
                level2EndPoint.put(EnumKey.S,new Endpoint(host,port));
            }else if("medium".equals(levelStr)){
                level2EndPoint.put(EnumKey.M,new Endpoint(host,port));
            }else {
                level2EndPoint.put(EnumKey.L,new Endpoint(host,port));
            }
            System.out.println(levelStr+"->"+host+":"+port);
        }
        return level2EndPoint;
    }
    public boolean findLsx() throws Exception {
        System.out.println("查找LSX");
        String strKey = MessageFormat.format("/{0}","lsx");
        ByteSequence key  = ByteSequence.fromString(strKey);
        GetResponse response = kv.get(key, GetOption.newBuilder().withPrefix(key).build()).get();
        if(response.getKvs().size()==0) return false;
        return true;

    }
    public Map<EnumKey,Endpoint> find(String serviceName, Map<EnumKey,Endpoint> level2EndPoint) throws Exception {

        String strKey = MessageFormat.format("/{0}",serviceName);
        ByteSequence key  = ByteSequence.fromString(strKey);
        GetResponse response = kv.get(key, GetOption.newBuilder().withPrefix(key).build()).get();

        //List<Endpoint> endpoints = new ArrayList<>();
        for (com.coreos.jetcd.data.KeyValue kv : response.getKvs()){
            String k = kv.getKey().toStringUtf8();
            int index = k.lastIndexOf("/");
            String levelStr = k.substring(index + 1,k.length());

            String v= kv.getValue().toStringUtf8();
            String host = v.split(":")[0];
            int port = Integer.valueOf(v.split(":")[1]);
            if("small".equals(levelStr)){
                level2EndPoint.put(EnumKey.S,new Endpoint(host,port));
            }else if("medium".equals(levelStr)){
                level2EndPoint.put(EnumKey.M,new Endpoint(host,port));
            }else {
                level2EndPoint.put(EnumKey.L,new Endpoint(host,port));
            }
            System.out.println(levelStr+"->"+host+":"+port);
        }
        return level2EndPoint;
    }
}