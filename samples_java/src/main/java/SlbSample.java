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
 * @description 负载均衡实例的创建、更新、查询、列举、删除
 */
public class SlbSample {

    /**
     * 云厂商
     */
    private final static String provider = "Aliyun";

    /**
     * 云产品
     */
    private final static String productCode = "SLB";

    /**
     * 资源类型
     */
    private final static String resourceTypeCode = "LoadBalancer";

    /**
     * 地域
     */
    private final static String regionId = "cn-beijing";

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

            //创建slb负载均衡实例
            String instanceId = createLoadBalancer();
            //更新slb负载均衡实例
            boolean update = updateLoadBalancer(instanceId);
            //查询slb负载均衡实例
            String instance = getLoadBalancer(instanceId);
            System.out.println(instance);
            //列举slb负载均衡实例
            String instances = listLoadBalancers();
            System.out.println(instances);
            //删除slb负载均衡实例
            boolean delete = deleteLoadBalancer(instanceId);
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
     * 创建slb负载均衡实例
     */
    private static String createLoadBalancer() throws Exception {

        //资源属性及其对应值（key，value的map转为String即可）
        Map<String, Object> resourceAttributeMap = new HashMap<>();
        //实例名称
        resourceAttributeMap.put("LoadBalancerName", "cc-test");
        //实例规格
        resourceAttributeMap.put("LoadBalancerSpec", "slb.s3.small");
        //公网类型实例的付费方式
        resourceAttributeMap.put("InternetChargeType", "PayByBandwidth");
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
     * 更新slb负载均衡实例
     */
    private static boolean updateLoadBalancer(String resourceId) throws Exception {

        String resourceAttributes = "{\"LoadBalancerName\":\"cc-test2\",\"DeleteProtection\":\"off\",\"ModificationProtectionStatus\":\"ConsoleProtection\",\"LoadBalancerSpec\":\"slb.s2.small\",\"Bandwidth\":\"5\",\"InternetChargeType\":\"PayByBandwidth\"}";
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
     * 查询slb负载均衡实例
     */
    private static String getLoadBalancer(String resourceId) throws Exception {

        GetResourceRequest getResourceRequest = new GetResourceRequest();
        //region化的产品必须加此参数，表示获取哪个region的资源
        getResourceRequest.setRegionId(regionId);
        GetResourceResponse getResourceResponse = cloudControlClient.getResource(provider, productCode, resourceTypeCode, resourceId, getResourceRequest);
        return getResourceResponse.getBody().getResource().getResourceAttributes();
    }

    /**
     * 列举slb负载均衡实例
     */
    private static String listLoadBalancers() throws Exception {

        ListResourcesRequest listResourceRequest = new ListResourcesRequest();
        List<String> regionIds = new ArrayList<>();
        regionIds.add(regionId);
        listResourceRequest.setRegionIds(regionIds);
        ListResourcesResponse listResourceResponse = cloudControlClient.listResources(provider, productCode, resourceTypeCode, listResourceRequest);
        return new Gson().toJson(listResourceResponse.getBody().getResources());
    }

    /**
     * 删除slb负载均衡实例
     */
    private static boolean deleteLoadBalancer(String resourceId) throws Exception {

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
