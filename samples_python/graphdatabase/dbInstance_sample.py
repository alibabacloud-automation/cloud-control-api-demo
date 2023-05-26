import time
from samples_python.task_status import TaskStatus
from alibabacloud_cloudcontrol20220830.client import Client as CloudControlClient
from alibabacloud_tea_openapi import models as open_api_models
from alibabacloud_cloudcontrol20220830 import models as cloud_control_models
from alibabacloud_tea_util.client import Client as UtilClient


class DbInstanceSample:

    def __init__(self):
        pass

    @staticmethod
    def create_client(
    ) -> CloudControlClient:
        """
        使用AK&SK初始化账号Client
        @return: Client
        @throws Exception
        """
        config = open_api_models.Config(
            # 您的AccessKey ID(改为自己的)
            access_key_id="ak",
            # 您的AccessKey Secret(改为自己的)
            access_key_secret="sk",
            # 公司UserAgent信息(改为自己的)
            user_agent="vendor/AnyCloudCompany-CloudManager@2.1.0",
            # 设置sdk读取超时时间
            read_timeout=20000
        )
        # 访问的域名
        config.endpoint = 'cloudcontrol.aliyuncs.com'
        return CloudControlClient(config)

    @staticmethod
    def create_dbInstance(
    ) -> str:
        """
        创建图数据库实例
        @return: resourceId
        @throws Exception
        """
        # 资源属性及其对应值（json字符串）
        resource_attribute_map = {
            "DbVersion": "1.0",
            "DbInstanceNetworkType": "VPC",
            "DbNodeStorage": 20,
            "DbInstanceStorageType": "cloud_essd",
            "DbInstanceDescription": "test",
            "DbNodeClass": "gdb.r.xlarge",
            "DbInstanceCategory": "ha",
            "ZoneId": "cn-hangzhou-h",
            "VpcId": "vpc-bp1bvsykm9f9hkfexxxxx",
            "VSwitchId": "vsw-bp152wgftimgq80exxxxx",
            "ResourceGroupId": "rg-acfmzaq3ypxxxxx",
            "DBInstanceIPArray": [
                {
                    "SecurityIps": "10.10.10.10"
                }
            ],
            "CreateType": "CreateDBInstance",
            "Tags": [
                {
                    "TagKey": "cckey1",
                    "TagValue": "ccvalue1"
                },
                {
                    "TagKey": "cckey2",
                    "TagValue": "ccvalue2"
                }
            ]
        }

        create_resource_request = cloud_control_models.CreateResourceRequest()
        create_resource_request.region_id = regionId
        create_resource_request.body = resource_attribute_map
        create_resources_path = request_path
        create_resource_response = cloud_control_client.create_resource(create_resources_path, create_resource_request)
        if create_resource_response.status_code == 201:
            return create_resource_response.body.resource_id
        elif create_resource_response.status_code == 202:
            # 获取异步超时时间
            timeout = float(create_resource_response.headers.get(timeout_key))
            start_time = time.time()
            get_task_response = None
            task_status = ''
            print(timeout)
            # 轮询异步创建任务
            while time.time() - start_time < timeout:
                get_task_response = cloud_control_client.get_task(create_resource_response.body.task_id)
                task_status = get_task_response.body.task.status
                if task_status != TaskStatus.RUNNING.value:
                    break
                time.sleep(period)

            # 异步任务状态判断
            if task_status == TaskStatus.Succeeded.value:
                return get_task_response.body.task.resource_id
            elif task_status == TaskStatus.FAILED.value:
                error = get_task_response.body.task.error
                print("errorCode is %s, errorMessage is %s" % (error.code, error.message))
                raise Exception("Task is failed!")
            elif task_status == TaskStatus.RUNNING.value:
                cloud_control_client.cancel_task(create_resource_response.body.task_id)
                raise Exception("Task is timeout!")
            else:
                raise Exception("Invalid Task status")
        else:
            raise Exception("Invalid status_code!")

    @staticmethod
    def update_dbInstance(
            resource_id: str
    ) -> bool:
        """
        更新图数据库实例
        @return: bool
        @throws Exception
        """
        # 资源属性及其对应值（json字符串）
        resource_attribute_map = {
            "ResourceGroupId": "rg-aek2xl5qajxxxxx",
            "SecurityResourceType": "DbInstance",
            "EcsSecurityGroupRelations": [
                {
                    "SecurityGroupId": "sg-bp1h8h2esmn7xupxxxxx"
                }
            ],
            "DbNodeStorage": 100,
            "DBInstanceIPArray": [
                {
                    "DbInstanceIpArrayName": "test1",
                    "SecurityIps": "10.10.10.10,11.11.11.11"
                },
                {
                    "DbInstanceIpArrayName": "default",
                    "SecurityIps": "10.10.10.10"
                }
            ],
            "DbInstanceDescription": "update",
            "Tags": [
                {
                    "TagKey": "cckey2",
                    "TagValue": "ccvalue2"
                },
                {
                    "TagKey": "cckey3",
                    "TagValue": "ccvalue3"
                }
            ]
        }

        update_resource_request = cloud_control_models.UpdateResourceRequest()
        update_resource_request.region_id = regionId
        update_resource_request.body = resource_attribute_map
        update_resources_path = request_path + "/" + resource_id
        update_resource_response = cloud_control_client.update_resource(update_resources_path, update_resource_request)
        if update_resource_response.status_code == 200:
            return True
        elif update_resource_response.status_code == 202:
            # 获取异步超时时间
            timeout = int(update_resource_response.headers.get(timeout_key))
            start_time = time.time()
            get_task_response = None
            task_status = ''
            # 轮询异步更新任务
            while time.time() - start_time < timeout:
                get_task_response = cloud_control_client.get_task(update_resource_response.body.task_id)
                task_status = get_task_response.body.task.status
                if task_status != TaskStatus.RUNNING.value:
                    break
                time.sleep(period)

            # 异步任务状态判断
            if task_status == TaskStatus.Succeeded.value:
                return True
            elif task_status == TaskStatus.FAILED.value:
                error = get_task_response.body.task.error
                print("errorCode is %s, errorMessage is %s" % (error.code, error.message))
                raise Exception("Task is failed!")
            elif task_status == TaskStatus.RUNNING.value:
                cloud_control_client.cancel_task(update_resource_response.body.task_id)
                raise Exception("Task is timeout!")
            else:
                raise Exception("Invalid Task status")
        else:
            raise Exception("Invalid status_code!")

    @staticmethod
    def get_dbInstance(
            resource_id: str
    ) -> cloud_control_models.GetResourcesResponseBodyResource:
        """
        查询图数据库实例
        @return: GetResourceResponseBodyResource
        @throws Exception
        """
        get_resource_request = cloud_control_models.GetResourcesRequest()
        get_resource_request.region_id = regionId
        get_resource_path = request_path + "/" + resource_id
        get_resource_response = cloud_control_client.get_resources(get_resource_path, get_resource_request)
        if get_resource_response.status_code == 200:
            return get_resource_response.body.resource
        else:
            raise Exception("Invalid status_code!")

    @staticmethod
    def list_dbInstances(
    ) -> list:
        """
        列举图数据库实例
        @return: list
        @throws Exception
        """
        list_resources_request = cloud_control_models.GetResourcesRequest()
        list_resources_request.region_id = regionId
        list_resources_path = request_path
        list_resources_response = cloud_control_client.get_resources(list_resources_path, list_resources_request)
        if list_resources_response.status_code == 200:
            return list_resources_response.body.resources
        else:
            raise Exception("Invalid status_code!")

    @staticmethod
    def delete_dbInstance(
            resource_id: str
    ) -> bool:
        """
        删除图数据库实例
        @return: bool
        @throws Exception
        """
        delete_resource_request = cloud_control_models.DeleteResourceRequest()
        delete_resource_request.region_id = regionId
        delete_resources_path = request_path + "/" + resource_id
        delete_resource_response = cloud_control_client.delete_resource(delete_resources_path, delete_resource_request)
        if delete_resource_response.status_code == 200:
            return True
        elif delete_resource_response.status_code == 202:
            # 获取异步超时时间
            timeout = int(delete_resource_response.headers.get(timeout_key))
            start_time = time.time()
            get_task_response = None
            task_status = ''
            # 轮询异步删除任务
            while time.time() - start_time < timeout:
                get_task_response = cloud_control_client.get_task(delete_resource_response.body.task_id)
                task_status = get_task_response.body.task.status
                if task_status != TaskStatus.RUNNING.value:
                    break
                time.sleep(period)

            # 异步任务状态判断
            if task_status == TaskStatus.Succeeded.value:
                return True
            elif task_status == TaskStatus.FAILED.value:
                error = get_task_response.body.task.error
                print("errorCode is %s, errorMessage is %s" % (error.code, error.message))
                raise Exception("Task is failed!")
            elif task_status == TaskStatus.RUNNING.value:
                cloud_control_client.cancel_task(delete_resource_response.body.task_id)
                raise Exception("Task is timeout!")
            else:
                raise Exception("Invalid Task status")
        else:
            raise Exception("Invalid status_code!")


if __name__ == '__main__':
    # 资源路径，格式为：/api/v1/providers/{provider}/products/{product}/resources/{parentResourcePath}/{resourceTypeCode}。
    request_path = "/api/v1/providers/Aliyun/products/GraphDatabase/resources/DbInstance"
    # 地域
    regionId = "cn-hangzhou"
    # 异步操作轮询间隔(s)
    period = 1
    # 超时时间header
    timeout_key = "x-acs-cloudcontrol-timeout"

    try:
        # 初始化sdk
        cloud_control_client = DbInstanceSample.create_client()
        # 创建资源
        resource_id = DbInstanceSample.create_dbInstance()
        resource = DbInstanceSample.get_dbInstance(resource_id)
        print(resource)
        # GraphDatabase实例创建后需等5分钟再变配，否则可能会出问题
        time.sleep(300)
        #更新资源
        update_success = DbInstanceSample.update_dbInstance(resource_id)
        # 查询资源
        resource2 = DbInstanceSample.get_dbInstance(resource_id)
        print(resource2)
        # 列举资源
        resources = DbInstanceSample.list_dbInstances()
        for item in resources:
            print(item)
        # 删除资源
        delete_success = DbInstanceSample.delete_dbInstance(resource_id)
    except Exception as error:
        # 打印 error
        print(UtilClient.assert_as_string(error.message))
