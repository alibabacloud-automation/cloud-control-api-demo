import com.alibaba.fastjson.JSON;
import com.aliyun.cloudcontrol20220830.Client;
import com.aliyun.cloudcontrol20220830.models.*;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * @author bzran
 * @description 查询资源meta
 */
public class ResourceMetaSample {

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
    private final static String resourceType = "Instance";

    /**
     * 数据源路径
     */
    private final static String dataSourcePath = "/api/v1/providers/aliyun/products/ECS/dataSources/Instance";

    /**
     * 资源类型路径
     */
    private final static String resourceTypePath = "/api/v1/providers/aliyun/products/ECS/resourceTypes/Instance";

    /**
     * SDK Client
     */
    private static com.aliyun.cloudcontrol20220830.Client cloudControlClient;

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

            //列举产品列表
            String products = listProducts();
            //列举ECS下的资源列表
            String resourceTypes = listResourceTypes(productCode);
            //查询ECS下的Instance资源
            String resourceType = getResourceType(resourceTypePath);
            //查询ECS下的Instance资源（国际化英文版本）
            String resourceTypeEn = getResourceTypeEn(resourceTypePath);
            //查询ECS的Instance资源支持的region
            String regions = listDataSources("RegionId");
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
     * 列举产品列表
     */
    private static String listProducts() throws Exception {
        ListProductsRequest listProductsRequest = new ListProductsRequest();
        ListProductsResponse listProductsResponse = cloudControlClient.listProducts(provider, listProductsRequest);
        return JSON.toJSONString(listProductsResponse.getBody().getProducts());
    }

    /**
     * 列举产品下的资源列表
     */
    private static String listResourceTypes(String product) throws Exception {
        ListResourceTypesRequest listResourceTypesRequest = new ListResourceTypesRequest();
        List<String> resourceTypes = new ArrayList<>();
        resourceTypes.add(resourceType);
        listResourceTypesRequest.setResourceTypes(resourceTypes);
        ListResourceTypesResponse listResourceTypesResponse = cloudControlClient.listResourceTypes(provider, product, listResourceTypesRequest);
        return JSON.toJSONString(listResourceTypesResponse.getBody().getResourceTypes());
    }

    /**
     * 查询资源meta详情
     */
    private static String getResourceType(String resourceTypePath) throws Exception {
        GetResourceTypeResponse getResourceTypeResponse = cloudControlClient.getResourceType(resourceTypePath);
        return JSON.toJSONString(getResourceTypeResponse.getBody().getResourceType());
    }

    /**
     * 查询资源meta详情(国际化英文版)
     */
    private static String getResourceTypeEn(String resourceTypePath) throws Exception {
//        Map<String, String> headers = new HashMap<>();
//        headers.put("x-acs-accept-language", "en_US");
        RuntimeOptions runtime = new RuntimeOptions();
        GetResourceTypeHeaders headers = new GetResourceTypeHeaders();
        headers.setXAcsAcceptLanguage("en_US");
        GetResourceTypeResponse getResourceTypeResponse = cloudControlClient.getResourceTypeWithOptions(resourceTypePath, headers, runtime);
        return JSON.toJSONString(getResourceTypeResponse.getBody().getResourceType());
    }

    /**
     * 查询datasource(支持地域、可用区等)
     */
    private static String listDataSources(String attributeName) throws Exception {
        ListDataSourcesRequest request = new ListDataSourcesRequest();
        request.setAttributeName(attributeName);
        ListDataSourcesResponse listDataSourcesResponse = cloudControlClient.listDataSources(dataSourcePath, request);
        return JSON.toJSONString(listDataSourcesResponse.getBody().getDataSources());
    }

}