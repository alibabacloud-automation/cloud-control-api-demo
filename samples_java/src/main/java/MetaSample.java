import com.aliyun.cloudcontrol20220606.Client;
import com.aliyun.cloudcontrol20220606.models.*;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;

/**
 * @author bzran
 * @description 查询资源meta
 */
public class MetaSample {

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
     * SDK Client
     */
    private static com.aliyun.cloudcontrol20220606.Client cloudControlClient;

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

            //列举产品列表
            String products = listProducts();
            //列举ECS下的资源列表
            String resourceTypes = listResourceTypes(productCode);
            //查询ECS的Instance资源meta
            String resourceType = getResourceType(productCode, resourceTypeCode);
            //查询ECS的Instance资源meta(国际化，返回英文描述)
            String resourceTypeEn = getResourceTypeEn(productCode, resourceTypeCode);
            //查询ECS的Instance资源支持的Region
            String regions = listDataSources(productCode, resourceTypeCode, "RegionId");
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
        return new Gson().toJson(listProductsResponse.getBody().getProducts());
    }

    /**
     * 列举产品下的资源列表
     */
    private static String listResourceTypes(String product) throws Exception {
        ListResourceTypesRequest listResourceTypesRequest = new ListResourceTypesRequest();
        ListResourceTypesResponse listResourceTypesResponse = cloudControlClient.listResourceTypes(provider, product, listResourceTypesRequest);
        return new Gson().toJson(listResourceTypesResponse.getBody().getResourceTypes());
    }

    /**
     * 查询资源meta详情
     */
    private static String getResourceType(String product, String resourceType) throws Exception {
        GetResourceTypeRequest getResourceTypeRequest = new GetResourceTypeRequest();
        GetResourceTypeResponse getResourceTypeResponse = cloudControlClient.getResourceType(provider, product, resourceType, getResourceTypeRequest);
        return new Gson().toJson(getResourceTypeResponse.getBody().getResourceType());
    }

    /**
     * 查询资源meta详情(国际化英文版)
     */
    private static String getResourceTypeEn(String product, String resourceType) throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-acs-accept-language", "en_US");
        RuntimeOptions runtime = new RuntimeOptions();
        GetResourceTypeRequest getResourceTypeRequest = new GetResourceTypeRequest();
        GetResourceTypeResponse getResourceTypeResponse = cloudControlClient.getResourceTypeWithOptions(provider, product, resourceType, getResourceTypeRequest, headers, runtime);
        return new Gson().toJson(getResourceTypeResponse.getBody().getResourceType());
    }

    /**
     * 查询datasource(支持地域、可用区等)
     */
    private static String listDataSources(String product, String resourceType, String attributeName) throws Exception {
        ListDataSourcesRequest request = new ListDataSourcesRequest();
        ListDataSourcesResponse listDataSourcesResponse = cloudControlClient.listDataSources(provider, product, resourceType, attributeName, request);
        return new Gson().toJson(listDataSourcesResponse.getBody().getDataSources());
    }

}