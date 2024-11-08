# Communication styles (RESTClient)

## Key Principles of REST

**REST** is an architectural style that leverages the existing protocols of the web, specifically HTTP. It emphasizes stateless communication and a uniform interface for resource manipulation. The core HTTP methods used in RESTful communication are:

- **GET**: Retrieve information from the server.
- **POST**: Create a new resource on the server.
- **PUT**: Update an existing resource.
- **DELETE**: Remove a resource from the server.

1. **Statelessness**: Each request from a client to the server contains all the necessary information. The server does not store client context between requests.
2. **Client-Server Separation**: The client and server are independent entities that communicate over the network, allowing for changes on either side without affecting the other.
3. **Resource Identification**: Resources are identified using URIs (Uniform Resource Identifiers), which can be represented in various formats, such as JSON or XML.
4. **Uniform Interface**: RESTful APIs provide a uniform interface, which simplifies and decouples the architecture, making it easier for clients to interact with resources.

## Building RESTful Services

To create a RESTful service in Spring Boot, follow these steps (tools/code/product-service-h2):

1. **Define a Model Class**:
   Create a model class that represents the data structure of the resource. For example, a `Product` class:

```java
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class Product {
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;
   @EqualsAndHashCode.Include
   private String uuid;
   private String name;
   private Double weight;

   public Product(String uuid, String name, Double weight) {
      this.uuid = uuid;
      this.name = name;
      this.weight = weight;
   }
}

```

2. **Create a REST Controller**:
   Define a REST controller that handles incoming HTTP requests and responds with the appropriate data.

```java
@RestController
@RequestMapping("/products")
public class ProductController {
   ProductService productService;

   public ProductController(ProductService productService) {
      this.productService = productService;
   }

   @GetMapping("/{uuid}")
   public Product findByUuid(@PathVariable String uuid) {
      return productService.findByUuid(uuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
   }

   @GetMapping
   public Iterable<Product> findAll() {
      return productService.findAll();
   }

   @PostMapping
   public Product create(@RequestBody Product product) {
      return productService.save(product);
   }

   @PutMapping("/{uuid}")
   public Product update(@PathVariable String uuid, @RequestBody Product product) {
      Optional<Product> optionalProject = productService.findByUuid(uuid);
      optionalProject.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
      product.setId(optionalProject.get().getId());
      return productService.save(product);
   }

   @DeleteMapping("/{uuid}")
   public void delete(@PathVariable String uuid) {
      Optional<Product> optionalProject = productService.findByUuid(uuid);
      optionalProject.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
      productService.delete(optionalProject.get());
   }
}
```

## Consuming RESTful Services with RestClient

In a microservices architecture, it is often necessary for services to consume APIs provided by other services. Spring Boot provides a simple way to achieve this through the `RestClient` class, which allows for HTTP requests to be made and handled effectively.

To consume a RESTful service in Spring Boot, follow these steps (code/sync-one-to-one):

1. **Use RestClient**:
   Use `RestClient` to make HTTP requests to other services. Here's an example of a service that consumes a REST API to fetch product data using a dedicated class named _ProductIntegration_:

```java
@RestController
@RequestMapping("/orders")
public class OrderController {
   OrderRepository orderRepository;
   ProductIntegration productIntegration;

   public OrderController(OrderRepository orderRepository, ProductIntegration productIntegration) {
      this.orderRepository = orderRepository;
      this.productIntegration = productIntegration;
   }

   @GetMapping(value = "")
   public Iterable<OrderDto> findAll() {
      Iterable<Order> orders = orderRepository.findAll();

      List<OrderDto> orderDtos = new ArrayList<>();
      for (Order order : orders) {
         OrderDto orderDto = new OrderDto(
                 order.getId(),
                 order.getUuid(),
                 order.getTimestamp(),
                 new HashSet<>()
         );

         for (ProductOrder productOrder : order.getProducts()) {
            orderDto.getProducts().add(productIntegration.findbyUuid(productOrder.getUuid()));
         }
         orderDtos.add(orderDto);
      }
      return orderDtos;
   }

   @GetMapping(value = "/{id}")
   public Order findById(@PathVariable Long id) {
      return orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));
   }
}
```

```java
@Component
public class ProductIntegration {
    String productServiceHost;
    int productServicePort;

    public ProductIntegration(
            @Value("${app.product-service.host}") String productServiceHost,
            @Value("${app.product-service.port}") int productServicePort) {
        this.productServiceHost = productServiceHost;
        this.productServicePort = productServicePort;
    }

    public List<ProductDto> findAll() {
        String url = "http://" + productServiceHost + ":" + productServicePort + "/products";
        RestClient restClient = RestClient.builder().build();
        return restClient.get()
                .uri(url)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public ProductDto findbyUuid(String uuid) {
        String url = "http://" + productServiceHost + ":" + productServicePort + "/products" + "/" + uuid;
        RestClient restClient = RestClient.builder().build();
        return restClient.get()
                .uri(url)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
```

When consuming external APIs, it is important to handle errors properly. You can use try-catch blocks to manage exceptions that may occur during API calls.

```
try {
    User user = restClient.getForObject(url, User.class);
} catch (RestClientException e) {
    // Handle the error, e.g., log it or throw a custom exception
}
```

## DTOs
TODO

## Resources
* https://www.baeldung.com/spring-boot-restclient