import time
from samples_python.task_status import TaskStatus
from alibabacloud_cloudcontrol20220830.client import Client as CloudControlClient
from alibabacloud_tea_openapi import models as open_api_models
from alibabacloud_cloudcontrol20220830 import models as cloud_control_models
from alibabacloud_tea_util.client import Client as UtilClient


class InstanceSample:

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
    def create_mysql_instance(
    ) -> str:
        """
        创建MySQL实例
        @return: resourceId
        @throws Exception
        """
        # 资源属性及其对应值（json字符串）
        resource_attribute_map = {
            "Category": "HighAvailability",
            "EngineVersion": "8.0",
            "ZoneId": "cn-beijing-i",
            "DbInstanceStorageType": "cloud_essd",
            "Engine": "MySQL",
            "ResourceGroupId": "rg-xxxxx",
            "VPCId": "vpc-xxxxx",
            "VSwitchId": "vsw-xxxxx",
            "InstanceNetworkType": "VPC",
            "DBInstanceStorage": 20,
            "ConnectionMode": "Standard",
            "DBInstanceDescription": "ccapi-test2",
            "StorageUpperBound": 2000,
            "StorageAutoScale": "Enable",
            "Tags": [
                {
                    "TagKey": "k1",
                    "TagValue": "v1"
                },
                {
                    "TagKey": "k2",
                    "TagValue": "v2"
                }
            ],
            "TargetMinorVersion": "rds_20221231",
            "StorageThreshold": 10,
            "DbIsIgnoreCase": "true",
            "DbTimeZone": "+08:00",
            "SecurityIPList": "10.10.10.10",
            "Port": "3306",
            "ZoneIdSlaveOne": "cn-beijing-l",
            "DbInstanceClass": "mysql.n2m.medium.2c",
            "DbInstanceNetType": "Intranet",
            "PaymentType": "PayAsYouGo"
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
    def create_postgresql_instance(
    ) -> str:
        """
        创建PostgreSQL实例
        @return: resourceId
        @throws Exception
        """
        # 资源属性及其对应值（json字符串）
        resource_attribute_map = {
            "Category": "HighAvailability",
            "ResourceGroupId": "rg-xxxxx",
            "DbInstanceStorageType": "cloud_essd",
            "InstanceNetworkType": "VPC",
            "SecurityIPList": "10.10.10.10",
            "DBInstanceStorage": 20,
            "Engine": "PostgreSQL",
            "DBInstanceDescription": "ccapi-test-postgre",
            "EngineVersion": "14.0",
            "ZoneId": "cn-beijing-i",
            "VPCId": "vpc-xxxxx",
            "VSwitchId": "vsw-xxxxx",
            "DbTimeZone": "Asia/Shanghai",
            "ZoneIdSlaveOne": "cn-beijing-l",
            "ConnectionMode": "Standard",
            "TargetMinorVersion": "rds_postgres_1400_20230530_14.6",
            "DbInstanceClass": "pg.n4.4c.2m",
            "DbInstanceNetType": "Intranet",
            "PaymentType": "PayAsYouGo"
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
    def get_instance(
            resource_id: str
    ) -> cloud_control_models.GetResourcesResponseBodyResource:
        """
        查询RDS实例
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
    def list_instances(
    ) -> list:
        """
        列举RDS实例
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
    def delete_instance(
            resource_id: str
    ) -> bool:
        """
        删除RDS实例
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
    request_path = "/api/v1/providers/Aliyun/products/RDS/resources/DBInstance"
    # 地域
    regionId = "cn-beijing"
    # 异步操作轮询间隔(s)
    period = 1
    # 超时时间header
    timeout_key = "x-acs-cloudcontrol-timeout"

    try:
        # 初始化sdk
        cloud_control_client = InstanceSample.create_client()
        # 创建MySQL实例
        mysql_resource_id = InstanceSample.create_mysql_instance()
        # 查询MySQL实例
        mysql_resource = InstanceSample.get_instance(mysql_resource_id)
        print(mysql_resource)
        # 创建PostgreSQL实例
        postgresql_resource_id = InstanceSample.create_postgresql_instance()
        # 查询PostgreSQL实例
        postgresql_resource = InstanceSample.get_instance(postgresql_resource_id)
        print(postgresql_resource)
        # 列举kafka实例
        resources = InstanceSample.list_instances()
        for item in resources:
            print(item)
        # 删除MySQL实例
        delete_mysql_success = InstanceSample.delete_instance(mysql_resource_id)
        # 删除PostgreSQL实例
        delete_postgresql_success = InstanceSample.delete_instance(postgresql_resource_id)
    except Exception as error:
        # 打印 error
        print(UtilClient.assert_as_string(error.message))