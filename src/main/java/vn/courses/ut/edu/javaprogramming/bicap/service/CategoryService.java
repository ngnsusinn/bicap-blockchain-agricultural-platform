package vn.courses.ut.edu.javaprogramming.bicap.service;

import vn.courses.ut.edu.javaprogramming.bicap.common.security.ActorAuthorizer;
import vn.courses.ut.edu.javaprogramming.bicap.dto.CategoryRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.CategoryResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Category;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ConflictException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.CategoryRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ProductRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Product category management (BICAP-5 / SRS-ADM-004): CRUD for the catalog of
 * categories used to classify products on the platform.
 */
@Service
@Transactional
@SuppressWarnings("null")
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           ProductRepository productRepository,
                           UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    private void checkView(String actorEmail) {
        ActorAuthorizer.requireAdminView(userRepository, actorEmail);
    }

    private void checkWrite(String actorEmail) {
        ActorAuthorizer.requireAdminWrite(userRepository, actorEmail);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories(String actorEmail) {
        checkView(actorEmail);
        List<Category> categories = categoryRepository.findAll();

        // One aggregate query for all product counts — avoids N+1 count calls per category.
        Map<Long, Long> counts = productRepository.countByCategory().stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        return categories.stream()
                .map(category -> toResponse(category, counts.getOrDefault(category.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id, String actorEmail) {
        checkView(actorEmail);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        long count = productRepository.countByCategory().stream()
                .filter(row -> row[0].equals(id))
                .mapToLong(row -> (Long) row[1])
                .sum();
        return toResponse(category, count);
    }

    @org.springframework.cache.annotation.CacheEvict(cacheNames = {
            vn.courses.ut.edu.javaprogramming.bicap.config.RedisCacheConfig.CACHE_CATEGORIES,
            vn.courses.ut.edu.javaprogramming.bicap.config.RedisCacheConfig.CACHE_MARKETPLACE_DETAIL },
            allEntries = true)
    public CategoryResponse createCategory(CategoryRequest request, String actorEmail) {
        checkWrite(actorEmail);

        String name = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Category name already exists: " + name);
        }

        Category category = Category.builder()
                .name(name)
                .description(trimToNull(request.getDescription()))
                .icon(trimToNull(request.getIcon()))
                .build();

        Category saved = categoryRepository.save(category);
        return toResponse(saved, 0L);
    }

    @org.springframework.cache.annotation.CacheEvict(cacheNames = {
            vn.courses.ut.edu.javaprogramming.bicap.config.RedisCacheConfig.CACHE_CATEGORIES,
            vn.courses.ut.edu.javaprogramming.bicap.config.RedisCacheConfig.CACHE_MARKETPLACE_DETAIL },
            allEntries = true)
    public CategoryResponse updateCategory(Long id, CategoryRequest request, String actorEmail) {
        checkWrite(actorEmail);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        // Partial updates (M-11): null fields are left untouched so a partial payload
        // never wipes existing data (e.g. icon).
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            String name = request.getName().trim();
            if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
                throw new ConflictException("Category name already exists: " + name);
            }
            category.setName(name);
        }
        if (request.getDescription() != null) {
            category.setDescription(trimToNull(request.getDescription()));
        }
        if (request.getIcon() != null) {
            category.setIcon(trimToNull(request.getIcon()));
        }

        Category saved = categoryRepository.save(category);
        long count = productRepository.countByCategory().stream()
                .filter(row -> row[0].equals(id))
                .mapToLong(row -> (Long) row[1])
                .sum();
        return toResponse(saved, count);
    }

    @org.springframework.cache.annotation.CacheEvict(cacheNames = {
            vn.courses.ut.edu.javaprogramming.bicap.config.RedisCacheConfig.CACHE_CATEGORIES,
            vn.courses.ut.edu.javaprogramming.bicap.config.RedisCacheConfig.CACHE_MARKETPLACE_DETAIL },
            allEntries = true)
    public void deleteCategory(Long id, String actorEmail) {
        checkWrite(actorEmail);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        // Guard: a category with associated products cannot be deleted — products are
        // required to have a category_id (NOT NULL), so deletion would orphan them.
        boolean hasProducts = productRepository.countByCategory().stream()
                .anyMatch(row -> row[0].equals(id) && (Long) row[1] > 0);
        if (hasProducts) {
            throw new BadRequestException("Cannot delete category \"" + category.getName()
                    + "\" because it has associated products");
        }
        categoryRepository.delete(category);
    }

    private CategoryResponse toResponse(Category category, long productCount) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription(),
                category.getIcon(), productCount, category.getCreatedAt());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
