package com.microservices.productcompositeservice.services;

import static org.springframework.http.HttpMethod.GET;

import com.api.core.product.Product;
import com.api.core.product.ProductService;
import com.api.core.recommendation.Recommendation;
import com.api.core.recommendation.RecommendationService;
import com.api.core.review.Review;
import com.api.core.review.ReviewService;
import com.api.exceptions.InvalidInputException;
import com.api.exceptions.NotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.util.http.HttpErrorInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ProductCompositeIntegration.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    private final String productServiceUrl;
    private final String recommendationServiceUrl;
    private final String reviewServiceUrl;

    public ProductCompositeIntegration(
            RestTemplate restTemplate,
            ObjectMapper mapper,
            @Value("${app.product-service.host}") String productServiceHost,
            @Value("${app.product-service.port}") int productServicePort,
            @Value("${app.recommendation-service.host}") String recommendationServiceHost,
            @Value("${app.recommendation-service.port}") int recommendationServicePort,
            @Value("${app.review-service.host}") String reviewServiceHost,
            @Value("${app.review-service.port}") int reviewServicePort
    ) {
        this.restTemplate = restTemplate;
        this.mapper = mapper;

        productServiceUrl = "http://" + productServiceHost + ":" + productServicePort + "/product/";
        recommendationServiceUrl = "http://" + recommendationServiceHost + ":" + recommendationServicePort + "/recommendation?productId=";
        reviewServiceUrl = "http://" + reviewServiceHost + ":" + reviewServicePort + "/review?productId=";
    }

    public Product getProduct(int productId) {
        try {
            String url = productServiceUrl + productId;
            log.debug("Will call getProduct API on URL: {}", url);

            Product product = restTemplate.getForObject(url, Product.class);
            log.debug("Found a product with id: {}", product.getProductId());

            return product;
        } catch (HttpClientErrorException ex) {
            switch (ex.getStatusCode()) {
                case NOT_FOUND:
                    throw new NotFoundException(getErrorMessage(ex));
                case UNPROCESSABLE_ENTITY:
                    throw new InvalidInputException(getErrorMessage(ex));
                default:
                    log.warn("Got an unexpected HTTP error: {}, will rethrow it", ex.getStatusCode());
                    log.warn("Error body: {}", ex.getResponseBodyAsString());
                    throw ex;
            }
        } catch (NullPointerException ex) {
            throw new NullPointerException(ex.getMessage());
        }
    }

    private String getErrorMessage(HttpClientErrorException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioex) {
            return ioex.getMessage();
        }
    }

    public List<Recommendation> getRecommendations(int productId) {
        try {
            String url = recommendationServiceUrl + productId;

            log.debug("Will call getRecommendations API on URL: {}", url);
            List<Recommendation> recommendations = restTemplate
                    .exchange(url, GET, null, new ParameterizedTypeReference<List<Recommendation>>() {})
                    .getBody();

            //log.debug("Found {} recommendations for a product with id: {}", recommendations.size(), productId);
            return recommendations;
        } catch (Exception ex) {
            log.warn("Got an exception while requesting recommendations, return zero recommendations: {}", ex.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Review> getReviews(int productId) {
        try {
            String url = reviewServiceUrl + productId;

            log.debug("Will call getReviews API on URL: {}", url);
            List<Review> reviews = restTemplate
                    .exchange(url, GET, null, new ParameterizedTypeReference<List<Review>>() {})
                    .getBody();

            log.debug("Found {} reviews for a product with id: {}", reviews.size(), productId);
            return reviews;
        } catch (Exception ex) {
            log.warn("Got an exception while requesting reviews, return zero reviews: {}", ex.getMessage());
            return new ArrayList<>();
        }
    }
}
