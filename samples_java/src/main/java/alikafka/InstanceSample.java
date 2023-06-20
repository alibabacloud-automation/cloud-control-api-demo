package alikafka;

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
 * @description 消息队列Kafka实例的创建、更新、查询、列举、删除
 */
public class InstanceSample {

    /**
     * 地域
     */
    private final static String regionId = "cn-beijing";

    /**
     * 资源路径，格式为：/api/v1/providers/{provider}/products/{product}/resources/{parentResourcePath}/{resourceTypeCode}。
     */
    private final static String resourcePath = "/api/v1/providers/Aliyun/products/AliKafka/resources/Instance";

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

            //创建kafka实例
            String resourceId = createInstance();
            //更新-部署kafka实例
            boolean start = updateInstanceStart(resourceId);
            //更新-升配kafka实例
            boolean upgrade = updateInstanceUpgrade(resourceId);
            //更新-修改kafka白名单
            boolean allowedIps = updateInstanceAllowedIps(resourceId);
            //更新-停用kafka实例
            boolean stop = updateInstanceStop(resourceId);
            //查询kafka实例
            String instance = getInstance(resourceId);
            System.out.println(instance);
            //列举kafka实例
            String instances = listInstances();
            System.out.println(instances);
            //删除kafka实例
            boolean delete = deleteInstance(resourceId);
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
     * 创建kafka实例
     */
    private static String createInstance() throws Exception {

        //资源属性及其对应值(json串,部分属性需改为自己账号对应的值)
        String resourceAttributes = "{\"DiskType\":0,\"DeployType\":\"4\",\"ResourceGroupId\":\"rg-acfmzygvt5xxxxx\",\"SpecType\":\"normal\",\"IoMaxSpec\":\"alikafka.hw.2xlarge\",\"DiskSize\":500,\"Tags\":[{\"TagKey\":\"k1\",\"TagValue\":\"v1\"},{\"TagKey\":\"k2\",\"TagValue\":\"v2\"}],\"EipMax\":3,\"PaymentType\":\"PayAsYouGo\",\"InstanceName\":\"create-test10\"}";
        //资源属性及其对应值
        Map<String, Object> resourceAttributeMap = JSONObject.parseObject(resourceAttributes,Map.class);

        CreateResourceRequest createResourceRequest = new CreateResourceRequest();
        createResourceRequest.setBody(resourceAttributeMap);
        createResourceRequest.setRegionId(regionId);
        String createResourcesPath = resourcePath;
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
            while (TaskStatusEnum.RUNNING.getValue().equals(taskStatus) && (System.currentTimeMillis() - timeStart) < timeout* 1000) {
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
     * 更新-部署kafka实例
     */
    private static boolean updateInstanceStart(String resourceId) throws Exception {

        String resourceAttributes = "{\"ResourceGroupId\":\"rg-aekzestgurxxxxx\",\"Tags\":[{\"TagKey\":\"k2\",\"TagValue\":\"v2\"},{\"TagKey\":\"k3\",\"TagValue\":\"v3\"}],\"Notifier\":\"331266\",\"DeployModule\":\"vpc\",\"IsSetUserAndPassword\":True,\"Status\":\"5\",\"SelectedZones\":\"zonei\",\"ZoneId\":\"zonei\",\"VSwitchId\":\"vsw-2zec5qkbafk06pwpxxxxx\",\"SecurityGroup\":\"sg-2zea338b38tog0xxxxxx\",\"IsEipInner\":True,\"VpcId\":\"vpc-2zep4j12i0ctkfs3xxxxx\",\"ServiceVersion\":\"2.2.0\",\"UserPhoneNum\":\"13120389576\",\"Password\":\"China1314\",\"Username\":\"Ljj12345\",\"InstanceName\":\"update-test\"}";
        UpdateResourceRequest updateResourceRequest = new UpdateResourceRequest();
        //资源属性及其对应值
        Map<String, Object> resourceAttributeMap = JSONObject.parseObject(resourceAttributes,Map.class);
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
     * 更新-升配kafka实例
     */
    private static boolean updateInstanceUpgrade(String resourceId) throws Exception {

        String resourceAttributes = "{\"DiskSize\":900,\"IoMaxSpec\":\"alikafka.hw.3xlarge\",\"SpecType\":\"professional\",\"EipMax\":6,\"EipModel\":True}";
        UpdateResourceRequest updateResourceRequest = new UpdateResourceRequest();
        //资源属性及其对应值
        Map<String, Object> resourceAttributeMap = JSONObject.parseObject(resourceAttributes,Map.class);
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
     * 更新-修改kafka白名单
     */
    private static boolean updateInstanceAllowedIps(String resourceId) throws Exception {

        String resourceAttributes = "{\"AllowedList\":{\"InternetList\":[{\"PortRange\":\"9093/9093\",\"AllowedIpList\":[\"192.168.0.0/16\",\"192.168.1.0/16\"]}],\"VpcList\":[{\"PortRange\":\"9092/9092\",\"AllowedIpList\":[\"192.168.0.0/21\",\"192.168.0.0/22\"]}],\"DeployType\":\"4\"}}";
        UpdateResourceRequest updateResourceRequest = new UpdateResourceRequest();
        //资源属性及其对应值
        Map<String, Object> resourceAttributeMap = JSONObject.parseObject(resourceAttributes,Map.class);
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
     * 更新-停用kafka实例
     */
    private static boolean updateInstanceStop(String resourceId) throws Exception {

        String resourceAttributes = "{\"Status\":\"15\"}";
        UpdateResourceRequest updateResourceRequest = new UpdateResourceRequest();
        //资源属性及其对应值
        Map<String, Object> resourceAttributeMap = JSONObject.parseObject(resourceAttributes,Map.class);
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
     * 查询kafka实例
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
     * 列举kafka实例
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
     * 删除kafka实例
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

