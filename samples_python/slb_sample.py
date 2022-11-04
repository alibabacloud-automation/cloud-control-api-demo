import json
import time
from samples_python.task_status import TaskStatus
from alibabacloud_cloudcontrol20220606.client import Client as CloudControlClient
from alibabacloud_tea_openapi import models as open_api_models
from alibabacloud_cloudcontrol20220606 import models as cloud_control_models
from alibabacloud_tea_util.client import Client as UtilClient


class SlbSample:

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
            access_key_id="your_ak",
            # 您的AccessKey Secret(改为自己的)
            access_key_secret="your_sk",
            # 公司UserAgent信息(改为自己的)
            user_agent="vendor/AnyCloudCompany-CloudManager@2.1.0",
            # 设置sdk读取超时时间
            read_timeout=20000
        )
        # 访问的域名
        config.endpoint = 'cloudcontrol.aliyuncs.com'
        return CloudControlClient(config)

    @staticmethod
    def create_loadbalancer(
    ) -> str:
        """
        创建负载均衡实例
        @return: resourceId
        @throws Exception
        """
        # 资源属性及其对应值（json字符串）
        resource_attribute_map = {"LoadBalancerName": "cc-test", "LoadBalancerSpec": "slb.s3.small",
                                  "InternetChargeType": "PayByBandwidth",
                                  "Tags": [{"TagKey": "cckey", "TagValue": "ccvalue"}]}
        resource_attributes = json.dumps(resource_attribute_map)

        create_resource_request = cloud_control_models.CreateResourceRequest()
        create_resource_request.region_id = regionId
        create_resource_request.body = resource_attributes
        create_resource_response = cloud_control_client.create_resource(provider, productCode, resourceTypeCode,
                                                                        create_resource_request)
        if create_resource_response.status_code == 201:
            return create_resource_response.body.resource_id
        elif create_resource_response.status_code == 202:
            start_time = time.time()
            get_task_response = None
            task_status = ''
            # 轮询异步创建任务
            while time.time() - start_time < create_resource_response.body.timeout:
                get_task_response = cloud_control_client.get_task(create_resource_response.body.task_id)
                task_status = get_task_response.body.task.status
                if task_status is not TaskStatus.RUNNING.value:
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
    def update_loadbalancer(
            resource_id: str
    ) -> bool:
        """
        更新负载均衡实例
        @return: bool
        @throws Exception
        """
        # 资源属性及其对应值（json字符串）
        resource_attribute_map = {'LoadBalancerName': "cc-test2", 'DeleteProtection': "off",
                                  'ModificationProtectionStatus': "ConsoleProtection",
                                  'LoadBalancerSpec': "slb.s2.small", 'Bandwidth': 5,
                                  "Tags": [{"TagKey": "cckey2", "TagValue": "ccvalue2"}]}
        resource_attributes = json.dumps(resource_attribute_map)

        update_resource_request = cloud_control_models.UpdateResourceRequest()
        update_resource_request.region_id = regionId
        update_resource_request.body = resource_attributes
        update_resource_response = cloud_control_client.update_resource(provider, productCode, resourceTypeCode,
                                                                        resource_id, update_resource_request)
        if update_resource_response.status_code == 200:
            return True
        elif update_resource_response.status_code == 202:
            start_time = time.time()
            get_task_response = None
            task_status = ''
            # 轮询异步创建任务
            while time.time() - start_time < update_resource_response.body.timeout:
                get_task_response = cloud_control_client.get_task(update_resource_response.body.task_id)
                task_status = get_task_response.body.task.status
                if task_status is not TaskStatus.RUNNING.value:
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
    def get_loadbalancer(
            resource_id: str
    ) -> cloud_control_models.GetResourceResponseBodyResource:
        """
        查询负载均衡实例
        @return: GetResourceResponseBodyResource
        @throws Exception
        """
        get_resource_request = cloud_control_models.GetResourceRequest()
        get_resource_request.region_id = regionId
        get_resource_response = cloud_control_client.get_resource(provider, productCode, resourceTypeCode,
                                                                  resource_id, get_resource_request)
        if get_resource_response.status_code == 200:
            return get_resource_response.body.resource
        else:
            raise Exception("Invalid status_code!")

    @staticmethod
    def list_loadbalancers(
    ) -> list:
        """
        列举负载均衡实例
        @return: list
        @throws Exception
        """
        list_resources_request = cloud_control_models.ListResourcesRequest()
        list_resources_request.region_ids = [regionId]
        filter_map = {'LoadBalancerName': "cc-test2"}
        list_resources_request.filter = filter_map
        list_resources_response = cloud_control_client.list_resources(provider, productCode, resourceTypeCode,
                                                                      list_resources_request)
        if list_resources_response.status_code == 200:
            return list_resources_response.body.resources
        else:
            raise Exception("Invalid status_code!")

    @staticmethod
    def delete_loadbalancer(
            resource_id: str
    ) -> bool:
        """
        删除负载均衡实例
        @return: bool
        @throws Exception
        """
        delete_resource_request = cloud_control_models.DeleteResourceRequest()
        delete_resource_request.region_id = regionId
        delete_resource_response = cloud_control_client.delete_resource(provider, productCode, resourceTypeCode,
                                                                        resource_id, delete_resource_request)
        if delete_resource_response.status_code == 200:
            return True
        elif delete_resource_response.status_code == 202:
            start_time = time.time()
            get_task_response = None
            task_status = ''
            # 轮询异步创建任务
            while time.time() - start_time < delete_resource_response.body.timeout:
                get_task_response = cloud_control_client.get_task(delete_resource_response.body.task_id)
                task_status = get_task_response.body.task.status
                if task_status is not TaskStatus.RUNNING.value:
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
    # 云厂商
    provider = "Aliyun"
    # 产品code
    productCode = "SLB"
    # 资源code
    resourceTypeCode = "LoadBalancer"
    # 地域
    regionId = "cn-beijing"
    # 异步操作轮询间隔(s)
    period = 1

    try:
        # 初始化sdk
        cloud_control_client = SlbSample.create_client()
        # 创建资源
        resource_id = SlbSample.create_loadbalancer()
        # 更新资源
        update_success = SlbSample.update_loadbalancer(resource_id)
        # 查询资源
        resource = SlbSample.get_loadbalancer(resource_id)
        print(resource)
        # 列举资源
        resources = SlbSample.list_loadbalancers()
        for item in resources:
            print(item)
        # 删除资源
        delete_success = SlbSample.delete_loadbalancer(resource_id)
    except Exception as error:
        # 打印 error
        print(UtilClient.assert_as_string(error.message))
