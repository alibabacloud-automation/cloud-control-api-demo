import com.aliyun.cloudcontrol20220606.Client;
import com.aliyun.cloudcontrol20220606.models.*;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author bzran
 * @description ECS实例的创建、更新、查询、列举、删除
 */
public class EcsSample {

    /**
     * 云厂商
     */
    private final static String provider = "Aliyun";

    /**
     * 云产品
     */
    private final static String productCode = "ECS";

    /**
     * 资源类型
     */
    private final static String resourceTypeCode = "Instance";

    /**
     * 地域
     */
    private final static String regionId = "cn-zhangjiakou";

    /**
     * SDK Client
     */
    private static com.aliyun.cloudcontrol20220606.Client cloudControlClient;

    /**
     * 异步操作轮询间隔(ms)
     */
    private static final int PERIOD = 1000;


    public static void main(String[] args) {

        try {
            Config config = new Config()
                    // 您的AccessKey ID
                    .setAccessKeyId("your_ak")
                    // 您的AccessKey Secret
                    .setAccessKeySecret("your_sk")
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

        //资源属性及其对应值（key，value的map转为String即可,部分属性需改为自己的值）
        Map<String, Object> resourceAttributeMap = new HashMap<>();
        //自己的镜像id
        resourceAttributeMap.put("ImageId", "centos_7_06_64_20G_alibase_20190711.vhd");
        //自己的安全组id
        resourceAttributeMap.put("SecurityGroupId", "sg-xxxxx");
        //自定义可用的实例规格
        resourceAttributeMap.put("InstanceType", "ecs.s6-c1m1.small");
        //自定义的实例名称
        resourceAttributeMap.put("InstanceName", "cc-test");
        //可用区id
        resourceAttributeMap.put("ZoneId", "cn-zhangjiakou-a");
        //系统磁盘信息
        Map<String, Object> systemDisk = new HashMap<>();
        systemDisk.put("Category", "cloud_efficiency");
        resourceAttributeMap.put("SystemDisk", systemDisk);
        //VPC网络信息（自己的VSwitchId）
        Map<String, Object> VpcAttributes = new HashMap<>();
        VpcAttributes.put("VSwitchId", "vsw-xxxxx");
        resourceAttributeMap.put("VpcAttributes", VpcAttributes);
        //Tag标签
        List<Map<String, String>> tags = new ArrayList<>();
        Map<String, String> tag1 = new HashMap<>();
        tag1.put("TagKey","cckey1");
        tag1.put("TagValue","ccvalue1");
        Map<String, String> tag2 = new HashMap<>();
        tag2.put("TagKey","cckey2");
        tag2.put("TagValue","ccvalue2");
        tags.add(tag1);
        tags.add(tag2);
        resourceAttributeMap.put("Tags", tags);
        String resourceAttributes = new Gson().toJson(resourceAttributeMap);
        CreateResourceRequest createResourceRequest = new CreateResourceRequest();
        createResourceRequest.setBody(resourceAttributes);
        createResourceRequest.setRegionId(regionId);
        //返回资源的id或对应taskid
        CreateResourceResponse createResourceResponse = cloudControlClient.createResource(provider, productCode, resourceTypeCode, createResourceRequest);
        String resourceId = null;
        if (createResourceResponse.statusCode == 201) {
            resourceId = createResourceResponse.getBody().resourceId;
        } else if (createResourceResponse.statusCode == 202) {
            String taskId = createResourceResponse.getBody().getTaskId();
            long timeStart = System.currentTimeMillis();
            GetTaskResponse getTaskResponse = cloudControlClient.getTask(taskId);
            String taskStatus = getTaskResponse.getBody().getTask().status;
            // 轮询异步创建任务
            while (TaskStatusEnum.RUNNING.getValue().equals(taskStatus) && (System.currentTimeMillis() - timeStart) < createResourceResponse.getBody().getTimeout() * 1000) {
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
            }else if (TaskStatusEnum.RUNNING.getValue().equals(taskStatus)){
                //超时则取消该任务
                cloudControlClient.cancelTask(taskId);
                throw new Exception("Task is timeout!");
            }else {
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

        String resourceAttributes = new Gson().toJson(resourceAttributeMap);
        UpdateResourceRequest updateResourceRequest = new UpdateResourceRequest();
        updateResourceRequest.setBody(resourceAttributes);
        updateResourceRequest.setRegionId(regionId);
        UpdateResourceResponse updateResourceResponse = cloudControlClient.updateResource(provider, productCode, resourceTypeCode, resourceId, updateResourceRequest);
        if (updateResourceResponse.statusCode == 200) {
            return true;
        } else if (updateResourceResponse.statusCode == 202) {
            String taskId = updateResourceResponse.getBody().getTaskId();
            long timeStart = System.currentTimeMillis();
            GetTaskResponse getTaskResponse = cloudControlClient.getTask(taskId);
            String taskStatus = getTaskResponse.getBody().getTask().status;
            // 轮询异步创建任务
            while (TaskStatusEnum.RUNNING.getValue().equals(taskStatus) && (System.currentTimeMillis() - timeStart) < updateResourceResponse.getBody().getTimeout() * 1000) {
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
            }else if (TaskStatusEnum.RUNNING.getValue().equals(taskStatus)){
                //超时则取消该任务
                cloudControlClient.cancelTask(taskId);
                throw new Exception("Task is timeout!");
            }else {
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

        String resourceAttributes = new Gson().toJson(resourceAttributeMap);
        UpdateResourceRequest updateResourceRequest = new UpdateResourceRequest();
        updateResourceRequest.setBody(resourceAttributes);
        updateResourceRequest.setRegionId(regionId);
        UpdateResourceResponse updateResourceResponse = cloudControlClient.updateResource(provider, productCode, resourceTypeCode, resourceId, updateResourceRequest);
        if (updateResourceResponse.statusCode == 200) {
            return true;
        } else if (updateResourceResponse.statusCode == 202) {
            String taskId = updateResourceResponse.getBody().getTaskId();
            long timeStart = System.currentTimeMillis();
            GetTaskResponse getTaskResponse = cloudControlClient.getTask(taskId);
            String taskStatus = getTaskResponse.getBody().getTask().status;
            // 轮询异步创建任务
            while (TaskStatusEnum.RUNNING.getValue().equals(taskStatus) && (System.currentTimeMillis() - timeStart) < updateResourceResponse.getBody().getTimeout() * 1000) {
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
            }else if (TaskStatusEnum.RUNNING.getValue().equals(taskStatus)){
                //超时则取消该任务
                cloudControlClient.cancelTask(taskId);
                throw new Exception("Task is timeout!");
            }else {
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

        String resourceAttributes = new Gson().toJson(resourceAttributeMap);
        UpdateResourceRequest updateResourceRequest = new UpdateResourceRequest();
        updateResourceRequest.setBody(resourceAttributes);
        updateResourceRequest.setRegionId(regionId);
        UpdateResourceResponse updateResourceResponse = cloudControlClient.updateResource(provider, productCode, resourceTypeCode, resourceId, updateResourceRequest);
        if (updateResourceResponse.statusCode == 200) {
            return true;
        } else if (updateResourceResponse.statusCode == 202) {
            String taskId = updateResourceResponse.getBody().getTaskId();
            long timeStart = System.currentTimeMillis();
            GetTaskResponse getTaskResponse = cloudControlClient.getTask(taskId);
            String taskStatus = getTaskResponse.getBody().getTask().status;
            // 轮询异步创建任务
            while (TaskStatusEnum.RUNNING.getValue().equals(taskStatus) && (System.currentTimeMillis() - timeStart) < updateResourceResponse.getBody().getTimeout() * 1000) {
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
            }else if (TaskStatusEnum.RUNNING.getValue().equals(taskStatus)){
                //超时则取消该任务
                cloudControlClient.cancelTask(taskId);
                throw new Exception("Task is timeout!");
            }else {
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

        GetResourceRequest getResourceRequest = new GetResourceRequest();
        //region化的产品必须加此参数，表示获取哪个region的资源
        getResourceRequest.setRegionId(regionId);
        //返回指定资源
        GetResourceResponse getResourceResponse = cloudControlClient.getResource(provider, productCode, resourceTypeCode, resourceId, getResourceRequest);
        return getResourceResponse.getBody().getResource().getResourceAttributes();
    }

    /**
     * 列举ecs实例
     */
    private static String listInstances() throws Exception {

        ListResourcesRequest listResourceRequest = new ListResourcesRequest();
        List<String> regionIds = new ArrayList<>();
        regionIds.add(regionId);
        listResourceRequest.setRegionIds(regionIds);
        //返回资源列表
        ListResourcesResponse listResourceResponse = cloudControlClient.listResources(provider, productCode, resourceTypeCode, listResourceRequest);
        return new Gson().toJson(listResourceResponse.getBody().getResources());
    }

    /**
     * 删除ecs实例
     */
    private static boolean deleteInstance(String resourceId) throws Exception {

        DeleteResourceRequest deleteResourceRequest = new DeleteResourceRequest();
        //region化的产品必须加此参数，表示删除哪个region的资源
        deleteResourceRequest.setRegionId(regionId);
        DeleteResourceResponse deleteResourceResponse = cloudControlClient.deleteResource(provider, productCode, resourceTypeCode, resourceId, deleteResourceRequest);
        if (deleteResourceResponse.statusCode == 200) {
            return true;
        } else if (deleteResourceResponse.statusCode == 202) {
            String taskId = deleteResourceResponse.getBody().getTaskId();
            long timeStart = System.currentTimeMillis();
            GetTaskResponse getTaskResponse = cloudControlClient.getTask(taskId);
            String taskStatus = getTaskResponse.getBody().getTask().status;
            // 轮询异步创建任务
            while (TaskStatusEnum.RUNNING.getValue().equals(taskStatus) && (System.currentTimeMillis() - timeStart) < deleteResourceResponse.getBody().getTimeout() * 1000) {
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
            }else if (TaskStatusEnum.RUNNING.getValue().equals(taskStatus)){
                //超时则取消该任务
                cloudControlClient.cancelTask(taskId);
                throw new Exception("Task is timeout!");
            }else {
                throw new Exception("Invalid Task status!");
            }
        } else {
            throw new Exception("Invalid status code!");
        }
    }

}
