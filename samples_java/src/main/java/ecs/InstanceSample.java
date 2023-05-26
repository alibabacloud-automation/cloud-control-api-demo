package ecs;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.cloudcontrol20220830.Client;
import com.aliyun.cloudcontrol20220830.models.*;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import enums.TaskStatusEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * @author bzran
 * @description ECS实例的创建、更新、查询、列举、删除
 */
public class InstanceSample {

    /**
     * 地域
     */
    private final static String regionId = "cn-zhangjiakou";

    /**
     * 资源路径
     */
    private final static String resourcePath = "/api/v1/providers/Aliyun/products/ECS/resources/Instance";

    /**
     * 超时时间header
     */
    private final static String timeoutKey = "x-acs-cloudcontrol-timeout";

    /**
     * SDK Client
     */
    private static com.aliyun.cloudcontrol20220830.Client cloudControlClient;

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

            //创建ecs实例
            String instanceId = createInstance();
            //查询ecs实例
            String instance = getInstance(instanceId);
            System.out.println(instance);
            //启动ecs实例（修改运行状态属性为Running）
            boolean start = startInstance(instanceId);
            //停止ecs实例（修改运行状态属性为Stopped）
            boolean stop = stopInstance(instanceId);
            //更新ecs实例（名称、实例规格，运行状态等属性均可修改）
            boolean update = updateInstance(instanceId);
            //列举ecs实例
            String instances = listInstances();
            System.out.println(instances);
            //ecs实例创建1分钟内无法删除，故等待一段时间再删除
            Thread.sleep(30000);
            //删除ecs实例
            boolean delete = deleteInstance(instanceId);
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
     * 创建ecs实例
     */
    private static String createInstance() throws Exception {

        //资源属性及其对应值(json串,部分属性需改为自己账号对应的值)
        String resourceAttributes = "{\"InstanceName\":\"cc-test\",\"SystemDisk\":{\"Category\":\"cloud_efficiency\"},\"ZoneId\":\"cn-zhangjiakou-a\",\"SecurityGroupId\":\"sg-8vb8b4g6z8wvxnqxxxxx\",\"ImageId\":\"centos_7_06_64_20G_alibase_20190711.vhd\",\"InstanceType\":\"ecs.s6-c1m1.small\",\"VpcAttributes\":{\"VSwitchId\":\"vsw-8vb68mghftv81ceaxxxxx\"},\"Tags\":[{\"TagKey\":\"cckey\",\"TagValue\":\"ccvalue\"}]}";
        //资源属性及其对应值
        Map<String, Object> resourceAttributeMap = JSONObject.parseObject(resourceAttributes,Map.class);
        CreateResourceRequest createResourceRequest = new CreateResourceRequest();
        createResourceRequest.setBody(resourceAttributeMap);
        createResourceRequest.setRegionId(regionId);
        String createResourcesPath = resourcePath;;
        //返回资源的id或对应taskid
        CreateResourceResponse createResourceResponse = cloudControlClient.createResource(createResourcesPath, createResourceRequest);
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
     * 启动ecs实例（修改运行状态为Running）
     */
    private static boolean startInstance(String resourceId) throws Exception {

        //资源属性及其对应值（key，value的map转为String即可）
        Map<String, Object> resourceAttributeMap = new HashMap<>();
        //实例名称
        resourceAttributeMap.put("Status", "Running");
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
            while (TaskStatusEnum.RUNNING.getValue().equals(taskStatus) && (System.currentTimeMillis() - timeStart) < timeout * 1000) {
                getTaskResponse = cloudControlClient.getTask(taskId);
                taskStatus = getTaskResponse.getBody().getTask().status;
                Thread.sleep(PERIOD);
            }

            //判断异步任务结果
            if (TaskStatusEnum.Succeeded.getValue().equals(taskStatus)) {
                //成功
                resourceId = getTaskResponse.getBody().getTask().resourceId;
                System.out.println("Asyn starting succeed.");
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
     * 更新ecs实例（名称、实例规格，运行状态等属性均可修改）
     */
    private static boolean updateInstance(String resourceId) throws Exception {

        //资源属性及其对应值（key，value的map转为String即可）
        Map<String, Object> resourceAttributeMap = new HashMap<>();
        //实例名称
        resourceAttributeMap.put("InstanceName", "cc-test2");
        //实例规格
        resourceAttributeMap.put("InstanceType", "ecs.s6-c1m2.small");
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
            while (TaskStatusEnum.RUNNING.getValue().equals(taskStatus) && (System.currentTimeMillis() - timeStart) < timeout * 1000) {
                getTaskResponse = cloudControlClient.getTask(taskId);
                taskStatus = getTaskResponse.getBody().getTask().status;
                System.out.println(taskStatus);
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
     * 停止ecs实例（修改运行状态为Stopped）
     */
    private static boolean stopInstance(String resourceId) throws Exception {

        //资源属性及其对应值（key，value的map转为String即可）
        Map<String, Object> resourceAttributeMap = new HashMap<>();
        //实例名称
        resourceAttributeMap.put("Status", "Stopped");
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
            while (TaskStatusEnum.RUNNING.getValue().equals(taskStatus) && (System.currentTimeMillis() - timeStart) < timeout * 1000) {
                getTaskResponse = cloudControlClient.getTask(taskId);
                taskStatus = getTaskResponse.getBody().getTask().status;
                Thread.sleep(PERIOD);
            }

            //判断异步任务结果
            if (TaskStatusEnum.Succeeded.getValue().equals(taskStatus)) {
                //成功
                resourceId = getTaskResponse.getBody().getTask().resourceId;
                System.out.println("Asyn stopping succeed.");
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
     * 查询ecs实例
     */
    private static String getInstance(String resourceId) throws Exception {

        GetResourcesRequest getResourceRequest = new GetResourcesRequest();
        //region化的产品必须加此参数，表示获取哪个region的资源
        getResourceRequest.setRegionId(regionId);
        String getResourcePath = resourcePath + "/" + resourceId;
        //返回指定资源
        GetResourcesResponse getResourceResponse = cloudControlClient.getResources(getResourcePath, getResourceRequest);
        return JSON.toJSONString(getResourceResponse.getBody().getResource().getResourceAttributes());
    }

    /**
     * 列举ecs实例
     */
    private static String listInstances() throws Exception {

        GetResourcesRequest getResourcesRequest = new GetResourcesRequest();
        getResourcesRequest.setRegionId(regionId);
        String listResourcesPath = resourcePath;
        //返回资源列表
        GetResourcesResponse getResourcesResponse = cloudControlClient.getResources(listResourcesPath, getResourcesRequest);
        return JSON.toJSONString(getResourcesResponse.getBody().getResources());
    }

    /**
     * 删除ecs实例
     */
    private static boolean deleteInstance(String resourceId) throws Exception {

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
