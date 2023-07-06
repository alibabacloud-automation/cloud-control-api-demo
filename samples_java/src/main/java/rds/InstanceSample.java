package rds;

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
 * @description RDS实例的创建、更新、查询、列举、删除
 */
public class InstanceSample {

    /**
     * 地域
     */
    private final static String regionId = "cn-beijing";

    /**
     * 资源路径，格式为：/api/v1/providers/{provider}/products/{product}/resources/{parentResourcePath}/{resourceTypeCode}。
     */
    private final static String resourcePath = "/api/v1/providers/Aliyun/products/RDS/resources/DBInstance";

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

            //创建MySQL实例
            String mysqlResourceId = createMysqlInstance();
            //查询MySQL实例
            String mysqlResource = getInstance(mysqlResourceId);
            System.out.println(mysqlResource);
            //创建PostgreSQL实例
            String postgresqlResourceId = createPostgresqlInstance();
            //查询PostgreSQL实例
            String postgresqlResource = getInstance(postgresqlResourceId);
            System.out.println(postgresqlResource);
            //列举slb负载均衡实例
            String resources = listInstances();
            System.out.println(resources);
            //删除MySQL实例
            boolean deleteMysql = deleteInstance(mysqlResourceId);
            //删除PostgreSQL实例
            boolean deletePostgresql = deleteInstance(mysqlResourceId);
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
     * 创建MySQL实例
     */
    private static String createMysqlInstance() throws Exception {

        //资源属性及其对应值(json串,部分属性需改为自己账号对应的值)
        String resourceAttributes = "{\"Category\":\"HighAvailability\",\"EngineVersion\":\"8.0\",\"ZoneId\":\"cn-beijing-i\",\"DbInstanceStorageType\":\"cloud_essd\",\"Engine\":\"MySQL\",\"ResourceGroupId\":\"rg-xxxxx\",\"VPCId\":\"vpc-xxxxx\",\"VSwitchId\":\"vsw-xxxxx\",\"InstanceNetworkType\":\"VPC\",\"DBInstanceStorage\":20,\"ConnectionMode\":\"Standard\",\"DBInstanceDescription\":\"ccapi-test2\",\"StorageUpperBound\":2000,\"StorageAutoScale\":\"Enable\",\"Tags\":[{\"TagKey\":\"k1\",\"TagValue\":\"v1\"},{\"TagKey\":\"k2\",\"TagValue\":\"v2\"}],\"TargetMinorVersion\":\"rds_20221231\",\"StorageThreshold\":10,\"DbIsIgnoreCase\":\"true\",\"DbTimeZone\":\"+08:00\",\"SecurityIPList\":\"10.10.10.10\",\"Port\":\"3306\",\"ZoneIdSlaveOne\":\"cn-beijing-l\",\"DbInstanceClass\":\"mysql.n2m.medium.2c\",\"DbInstanceNetType\":\"Intranet\",\"PaymentType\":\"PayAsYouGo\"}";
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
     * 创建PostgreSQL实例
     */
    private static String createPostgresqlInstance() throws Exception {

        //资源属性及其对应值(json串,部分属性需改为自己账号对应的值)
        String resourceAttributes = "{\"Category\":\"HighAvailability\",\"ResourceGroupId\":\"rg-xxxxx\",\"DbInstanceStorageType\":\"cloud_essd\",\"InstanceNetworkType\":\"VPC\",\"SecurityIPList\":\"10.10.10.10\",\"DBInstanceStorage\":20,\"Engine\":\"PostgreSQL\",\"DBInstanceDescription\":\"ccapi-test-postgre\",\"EngineVersion\":\"14.0\",\"ZoneId\":\"cn-beijing-i\",\"VPCId\":\"vpc-xxxxx\",\"VSwitchId\":\"vsw-xxxxx\",\"DbTimeZone\":\"Asia/Shanghai\",\"ZoneIdSlaveOne\":\"cn-beijing-l\",\"ConnectionMode\":\"Standard\",\"TargetMinorVersion\":\"rds_postgres_1400_20230530_14.6\",\"DbInstanceClass\":\"pg.n4.4c.2m\",\"DbInstanceNetType\":\"Intranet\",\"PaymentType\":\"PayAsYouGo\"}";
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
     * 查询RDS实例
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
     * 列举RDS实例
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
     * 删除RDS实例
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

