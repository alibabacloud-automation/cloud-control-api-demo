from alibabacloud_cloudcontrol20220830.client import Client as CloudControlClient
from alibabacloud_tea_openapi import models as open_api_models
from alibabacloud_cloudcontrol20220830 import models as cloud_control_models
from alibabacloud_tea_util import models as util_models
from alibabacloud_tea_util.client import Client as UtilClient


class ResourceMetaSample:

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
    def list_products(
    ) -> list:
        """
        列举产品列表
        @return: list
        @throws Exception
        """
        list_products_request = cloud_control_models.ListProductsRequest()
        list_products_response = cloud_control_client.list_products(provider, list_products_request)
        if list_products_response.status_code == 200:
            return list_products_response.body.products
        else:
            raise Exception("Invalid status_code!")

    @staticmethod
    def list_resource_types(
        product: str
    ) -> list:
        """
        列举产品下的资源列表
        @return: list
        @throws Exception
        """
        list_resource_types_request = cloud_control_models.ListResourceTypesRequest()
        list_resource_types_response = cloud_control_client.list_resource_types(provider, product,
                                                                                list_resource_types_request)
        if list_resource_types_response.status_code == 200:
            return list_resource_types_response.body.resource_types
        else:
            raise Exception("Invalid status_code!")

    @staticmethod
    def get_resource_type(
    ) -> cloud_control_models.GetResourceTypeResponseBodyResourceType:
        """
        查询资源meta详情
        @return: GetResourceTypeResponseBodyResourceType
        @throws Exception
        """
        get_resource_type_response = cloud_control_client.get_resource_type(resourceTypePath)
        if get_resource_type_response.status_code == 200:
            return get_resource_type_response.body.resource_type
        else:
            raise Exception("Invalid status_code!")

    @staticmethod
    def get_resource_type_en(
    ) -> cloud_control_models.GetResourceTypeResponseBodyResourceType:
        """
        查询资源meta详情(国际化英文版)
        @return: str
        @throws Exception
        """
        headers = cloud_control_models.GetResourceTypeHeaders()
        headers.x_acs_accept_language = "en_US"
        runtime = util_models.RuntimeOptions()
        get_resource_type_response = cloud_control_client.get_resource_type_with_options(
            resourceTypePath, headers, runtime)
        if get_resource_type_response.status_code == 200:
            return get_resource_type_response.body.resource_type
        else:
            raise Exception("Invalid status_code!")

    @staticmethod
    def list_data_source(
        attribute_name: str
    ) -> list:
        """
        查询datasource(支持地域、可用区等)
        @return: list
        @throws Exception
        """
        list_data_source_request = cloud_control_models.ListDataSourcesRequest()
        list_data_source_request.attribute_name = attribute_name
        list_data_source_response = cloud_control_client.list_data_sources(dataSourcePath, list_data_source_request)
        if list_data_source_response.status_code == 200:
            return list_data_source_response.body.data_sources
        else:
            raise Exception("Invalid status_code!")


if __name__ == '__main__':
    # 云厂商
    provider = "Aliyun"
    # 产品code
    product = "ECS"
    # 资源code
    resourceType = "Instance"
    # 资源类型路径
    resourceTypePath = "/api/v1/providers/aliyun/products/ECS/resourceTypes/Instance"
    # 数据源路径
    dataSourcePath = "/api/v1/providers/aliyun/products/ECS/dataSources/Instance"

    try:
        # 初始化sdk
        cloud_control_client = ResourceMetaSample.create_client()
        # 列举产品列表
        products = ResourceMetaSample.list_products()
        # 列举ECS下的资源列表
        resource_types = ResourceMetaSample.list_resource_types(product)
        # 查询ECS的Instance资源meta
        resource_type = ResourceMetaSample.get_resource_type()
        # 查询ECS的Instance资源meta(国际化，返回英文描述)
        resource_type_en = ResourceMetaSample.get_resource_type_en()
        # 查询ECS的Instance资源支持的Region
        data_sources = ResourceMetaSample.list_data_source('RegionId')
    except Exception as error:
        # 打印 error
        print(UtilClient.assert_as_string(error.message))
