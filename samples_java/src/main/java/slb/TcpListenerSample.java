package slb;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.cloudcontrol20220830.Client;
import com.aliyun.cloudcontrol20220830.models.*;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import enums.TaskStatusEnum;

import java.util.Map;


/**
 * @author bzran
 * @description 负载均衡TCP监听的创建、更新、查询、列举、删除
 */
public class TcpListenerSample {

    /**
     * 地域
     */
    private final static String regionId = "cn-zhangjiakou";


    /**
     * 资源路径，格式为：/api/v1/providers/{provider}/products/{product}/resources/{parentResourcePath}/{resourceTypeCode}。
     */
    private final static String resourcePath = "/api/v1/providers/Aliyun/products/SLB/resources/LoadBalancer/lb-2ze9i2ygwiw7snmtxxxxx/LoadBalancerTCPListener";

    /**
     * 超时时间header
     */
    private final static String timeoutKey = "x-acs-cloudcontrol-timeout";

    /**
     * SDK Client
     */
    private static Client cloudControlClient;

    /**
     * 异步操作轮询间隔(ms)
     */
    private static final int PERIOD = 1000;

    public static void main(String[] args) {

        try {
            Config config = new Config()
                    // 您的AccessKey ID
                    .setAccessKeyId("ak")
                    // 您的AccessKey Secret
                    .setAccessKeySecret("sk")
                    //设置sdk超时时间
                    .setReadTimeout(20000);
            config.endpoint = "cloudcontrol.aliyuncs.com";
            cloudControlClient = new Client(config);

            //创建Redis实例
            String instanceId = createTcpListener();
            //更新Redis实例
            boolean update = updateTcpListener(instanceId);
            //查询Redis实例
            String instance = getTcpListener(instanceId);
            System.out.println(instance);
            //列举Redis实例
            String instances = listTcpListeners();
            System.out.println(instances);
            //删除Redis实例
            boolean delete = deleteTcpListener(instanceId);
        } catch (TeaException e) {
            // 如有需要，请打印 error
            System.out.println("Error code: " + e.getCode());
            System.out.println("Error message: " + e.getMessage());
        } catch (Exception e) {
            TeaException error = new TeaException(e.getMessage(), e);
            // 打印 error
            System.out.println(com.aliyun.teautil.Common.assertAsString(error.message));
        }
    }

    /**
     * 创建负载均衡TCP监听
     */
    private static String createTcpListener() throws Exception {

        //资源属性及其对应值(json串,部分属性需改为自己账号对应的值)
        String resourceAttributes = "{\"Description\":\"cc-test\",\"ProxyProtocolV2Enabled\":False,\"Scheduler\":\"wrr\",\"HealthCheck\":\"on\",\"PersistenceTimeout\":0,\"Bandwidth\":100,\"HealthCheckHttpCode\":\"http_2xx\",\"EstablishedTimeout\":500,\"ListenerPort\":80,\"HealthCheckConnectTimeout\":5,\"AclStatus\":\"on\",\"HealthyThreshold\":3,\"HealthCheckType\":\"http\",\"UnhealthyThreshold\":3,\"HealthCheckUri\":\"/test/index.html\",\"BackendServerPort\":80,\"ConnectionDrainTimeout\":300,\"HealthCheckConnectPort\":80,\"HealthCheckInterval\":3,\"AclId\":\"acl-2zekr7blg91gk74rxxxxx\",\"ConnectionDrain\":\"on\",\"HealthCheckDomain\":\"127.0.0.1\",\"AclType\":\"white\",\"RegionId\":\"cn-beijing\",\"VServerGroupId\":\"rsp-2zemjamqxxxxx\"}";
        //资源属性及其对应值
        Map<String, Object> resourceAttributeMap = JSONObject.parseObject(resourceAttributes,Map.class);
        CreateResourceRequest createResourceRequest = new CreateResourceRequest();
        createResourceRequest.setBody(resourceAttributeMap);
        createResourceRequest.setRegionId(regionId);
        CreateResourceResponse createResourceResponse = cloudControlClient.createResource(resourcePath, createResourceRequest);
        String resourceId = null;
        if (createResourceResponse.statusCode == 201) {
            resourceId = createResourceResponse.getBody().resourceId;
        } else if (createResourceResponse.statusCode == 202) {
            String taskId = createResourceResponse.getBody().getTaskId();
            long timeStart = System.currentTimeMillis();
            GetTaskResponse getTaskResponse = cloudControlClient.getTask(taskId);
            String taskStatus = getTaskResponse.getBody().getTask().status;
            //获取异步超时时间
            int timeout = Integer.parseInt(createResourceResponse.getHeaders().get(timeoutKey));
            // 轮询异步创建任务
            while (TaskStatusEnum.RUNNING.getValue().equals(taskStatus) && (System.currentTimeMillis() - timeStart) < timeout * 1000) {
                getTaskResponse = cloudControlClient.getTask(taskId);
                taskStatus = getTaskResponse.getBody().getTask().status;
                Thread.sleep(PERIOD);
            }

            //判断异步任务结果
            if (TaskStatusEnum.Succeeded.getValue().equals(taskStatus)) {
                //成功
                resourceId = getTaskResponse.getBody().getTask().resourceId;
                System.out.println("Asyn Creating succeed.");
            } else if (TaskStatusEnum.FAILED.getValue().equals(taskStatus)) {
                //失败输出错误信息
                GetTaskResponseBody.GetTaskResponseBodyTaskError error = getTaskResponse.getBody().getTask().getError();
                System.out.println(String.format("errorCode is %s, errorMessage is %s.", error.getCode(), error.getMessage()));
                throw new Exception("Asyn Creating failed.");
            } else if (TaskStatusEnum.RUNNING.getValue().equals(taskStatus)) {
                //超时则取消该任务
                cloudControlClient.cancelTask(taskId);
                throw new Exception("Task is timeout!");
            } else {
                throw new Exception("Invalid Task status!");
            }
        } else {
            throw new Exception("Invalid status code!");
        }
        return resourceId;
    }

    /**
     * 更新负载均衡TCP监听
     */
    private static boolean updateTcpListener(String resourceId) throws Exception {

        //资源属性及其对应值(json串,部分属性需改为自己账号对应的值)
        String resourceAttributes = "{\"Description\":\"cc-test2\",\"ProxyProtocolV2Enabled\":True,\"UnhealthyThreshold\":5,\"Scheduler\":\"rr\",\"HealthCheckUri\":\"/testtest/index.html\",\"HealthCheck\":\"on\",\"PersistenceTimeout\":100,\"Bandwidth\":200,\"HealthCheckHttpCode\":\"http_4xx\",\"EstablishedTimeout\":900,\"HealthCheckConnectTimeout\":10,\"AclStatus\":\"on\",\"HealthyThreshold\":5,\"HealthCheckDomain\":\"127.0.0.2\",\"AclType\":\"black\",\"HealthCheckType\":\"http\",\"AclId\":\"acl-2zekomcx5igvomfrxxxxx\",\"SynProxy\":\"disable\",\"ConnectionDrainTimeout\":100,\"HealthCheckConnectPort\":81,\"Status\":\"running\",\"HealthCheckInterval\":5,\"ConnectionDrain\":\"on\",\"VServerGroupId\":\"rsp-2ze4k3chxxxxx\",\"VServerGroup\":\"on\"}";
        //资源属性及其对应值
        Map<String, Object> resourceAttributeMap = JSONObject.parseObject(resourceAttributes,Map.class);
        UpdateResourceRequest updateResourceRequest = new UpdateResourceRequest();
        updateResourceRequest.setBody(resourceAttributeMap);
        updateResourceRequest.setRegionId(regionId);
        String updateResourcePath = resourcePath + "/" + resourceId;
        UpdateResourceResponse updateResourceResponse = cloudControlClient.updateResource(updateResourcePath, updateResourceRequest);
        if (updateResourceResponse.statusCode == 200) {
            return true;
        } else if (updateResourceResponse.statusCode == 202) {
            String taskId = updateResourceResponse.getBody().getTaskId();
            long timeStart = System.currentTimeMillis();
            GetTaskResponse getTaskResponse = cloudControlClient.getTask(taskId);
            String taskStatus = getTaskResponse.getBody().getTask().status;
            //获取异步超时时间
            int timeout = Integer.parseInt(updateResourceResponse.getHeaders().get(timeoutKey));
            // 轮询异步创建任务
            while (TaskStatusEnum.RUNNING.getValue().equals(taskStatus) && (System.currentTimeMillis() - timeStart) < timeout* 1000) {
                getTaskResponse = cloudControlClient.getTask(taskId);
                taskStatus = getTaskResponse.getBody().getTask().status;
                Thread.sleep(PERIOD);
            }

            //判断异步任务结果
            if (TaskStatusEnum.Succeeded.getValue().equals(taskStatus)) {
                //成功
                resourceId = getTaskResponse.getBody().getTask().resourceId;
                System.out.println("Asyn updating succeed.");
                return true;
            } else if (TaskStatusEnum.FAILED.getValue().equals(taskStatus)) {
                //失败输出错误信息
                GetTaskResponseBody.GetTaskResponseBodyTaskError error = getTaskResponse.getBody().getTask().getError();
                System.out.println(String.format("errorCode is %s, errorMessage is %s.", error.getCode(), error.getMessage()));
                return false;
            } else if (TaskStatusEnum.RUNNING.getValue().equals(taskStatus)) {
                //超时则取消该任务
                cloudControlClient.cancelTask(taskId);
                throw new Exception("Task is timeout!");
            } else {
                throw new Exception("Invalid Task status!");
            }
        } else {
            throw new Exception("Invalid status code!");
        }
    }

    /**
     * 查询负载均衡TCP监听
     */
    private static String getTcpListener(String resourceId) throws Exception {

        GetResourcesRequest getResourceRequest = new GetResourcesRequest();
        //region化的产品必须加此参数，表示获取哪个region的资源
        getResourceRequest.setRegionId(regionId);
        String getResourcePath = resourcePath + "/" + resourceId;
        //返回指定资源
        GetResourcesResponse getResourceResponse = cloudControlClient.getResources(getResourcePath, getResourceRequest);
        return JSON.toJSONString(getResourceResponse.getBody().getResource().getResourceAttributes());
    }

    /**
     * 列举负载均衡TCP监听
     */
    private static String listTcpListeners() throws Exception {

        GetResourcesRequest getResourcesRequest = new GetResourcesRequest();
        getResourcesRequest.setRegionId(regionId);
        String listResourcesPath = resourcePath;
        //返回资源列表
        GetResourcesResponse getResourcesResponse = cloudControlClient.getResources(listResourcesPath, getResourcesRequest);
        return JSON.toJSONString(getResourcesResponse.getBody().getResources());
    }

    /**
     * 删除负载均衡TCP监听
     */
    private static boolean deleteTcpListener(String resourceId) throws Exception {

        DeleteResourceRequest deleteResourceRequest = new DeleteResourceRequest();
        //region化的产品必须加此参数，表示删除哪个region的资源
        deleteResourceRequest.setRegionId(regionId);
        String deleteResourcePath = resourcePath + "/" + resourceId;
        DeleteResourceResponse deleteResourceResponse = cloudControlClient.deleteResource(deleteResourcePath, deleteResourceRequest);
        if (deleteResourceResponse.statusCode == 200) {
            return true;
        } else if (deleteResourceResponse.statusCode == 202) {
            String taskId = deleteResourceResponse.getBody().getTaskId();
            long timeStart = System.currentTimeMillis();
            GetTaskResponse getTaskResponse = cloudControlClient.getTask(taskId);
            String taskStatus = getTaskResponse.getBody().getTask().status;
            //获取异步超时时间
            int timeout = Integer.parseInt(deleteResourceResponse.getHeaders().get(timeoutKey));
            // 轮询异步创建任务
            while (TaskStatusEnum.RUNNING.getValue().equals(taskStatus) && (System.currentTimeMillis() - timeStart) < timeout * 1000) {
                getTaskResponse = cloudControlClient.getTask(taskId);
                taskStatus = getTaskResponse.getBody().getTask().status;
                Thread.sleep(PERIOD);
            }

            //判断异步任务结果
            if (TaskStatusEnum.Succeeded.getValue().equals(taskStatus)) {
                //成功
                resourceId = getTaskResponse.getBody().getTask().resourceId;
                System.out.println("Asyn deleting succeed.");
                return true;
            } else if (TaskStatusEnum.FAILED.getValue().equals(taskStatus)) {
                //失败输出错误信息
                GetTaskResponseBody.GetTaskResponseBodyTaskError error = getTaskResponse.getBody().getTask().getError();
                System.out.println(String.format("errorCode is %s, errorMessage is %s.", error.getCode(), error.getMessage()));
                return false;
            } else if (TaskStatusEnum.RUNNING.getValue().equals(taskStatus)) {
                //超时则取消该任务
                cloudControlClient.cancelTask(taskId);
                throw new Exception("Task is timeout!");
            } else {
                throw new Exception("Invalid Task status!");
            }
        } else {
            throw new Exception("Invalid status code!");
        }
    }

}


