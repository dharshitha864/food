package com.foodexpress.config;

import com.foodexpress.entity.*;
import com.foodexpress.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Configuration
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final FoodCategoryRepository categoryRepository;
    private final FoodItemRepository foodItemRepository;
    private final CouponRepository couponRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           RestaurantRepository restaurantRepository,
                           FoodCategoryRepository categoryRepository,
                           FoodItemRepository foodItemRepository,
                           CouponRepository couponRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.categoryRepository = categoryRepository;
        this.foodItemRepository = foodItemRepository;
        this.couponRepository = couponRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // Baseline data already seeded
        }

        // 1. Seed Users
        User admin = new User(null, "Platform Administrator", "admin@foodexpress.com",
                passwordEncoder.encode("Admin@123"), "9876543210", Role.ROLE_SYSTEM_ADMIN, true);
        userRepository.save(admin);

        User restAdmin = new User(null, "Chef Rajesh Kumar", "manager@spicytreats.com",
                passwordEncoder.encode("Partner@123"), "9876543211", Role.ROLE_RESTAURANT_ADMIN, true);
        userRepository.save(restAdmin);

        User courier = new User(null, "Vikram Delivery Hero", "courier@foodexpress.com",
                passwordEncoder.encode("Courier@123"), "9876543212", Role.ROLE_DELIVERY_PARTNER, true);
        userRepository.save(courier);

        User customer = new User(null, "Ananya Sharma", "customer@foodexpress.com",
                passwordEncoder.encode("Customer@123"), "9876543213", Role.ROLE_CUSTOMER, true);
        userRepository.save(customer);

        // 2. Seed Categories
        FoodCategory catBiryani = categoryRepository.save(new FoodCategory(null, "Biryani & Curries",
                "Authentic royal Indian rice dishes and aromatic rich curries",
                "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=500"));

        FoodCategory catBurgers = categoryRepository.save(new FoodCategory(null, "Burgers & Sandwiches",
                "Gourmet burgers stacked high with crispy patties and fresh toppings",
                "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500"));

        FoodCategory catPizza = categoryRepository.save(new FoodCategory(null, "Pizza & Italian",
                "Woodfired sourdough pizzas loaded with melted mozzarella",
                "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=500"));

        FoodCategory catDesserts = categoryRepository.save(new FoodCategory(null, "Desserts & Shakes",
                "Decadent sweet treats and rich hand-spun milkshakes",
                "https://images.unsplash.com/photo-1551024709-8f23befc6f87?w=500"));

        // 3. Seed Restaurants
        Restaurant r1 = restaurantRepository.save(new Restaurant(null, restAdmin, "Spicy Treats Indian Bistro",
                "Royal Mughlai delicacies, clay-oven tandoor kebabs and signature dum biryanis",
                "124 Heritage Lane, Connaught Place", "011-23456789", "info@spicytreats.com", 4.8, true,
                "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=600"));

        Restaurant r2 = restaurantRepository.save(new Restaurant(null, restAdmin, "Burger Haven & Grill",
                "Juicy craft burgers, crispy golden fries and thick milkshakes",
                "45 Park Boulevard, Cyber City", "011-98765432", "hello@burgerhaven.com", 4.6, true,
                "https://images.unsplash.com/photo-1550547660-d9450f859349?w=600"));

        Restaurant r3 = restaurantRepository.save(new Restaurant(null, restAdmin, "Bella Italia Pizzeria",
                "Artisan thin-crust pizzas, handmade pastas and authentic Italian gelato",
                "88 Piazza Central, Indiranagar", "080-45678901", "ciao@bellaitalia.com", 4.7, true,
                "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=600"));

        // 4. Seed Food Items
        // Spicy Treats
        foodItemRepository.save(new FoodItem(null, r1, catBiryani, "Hyderabadi Dum Biryani",
                "Fragrant basmati rice slow-cooked with aromatic spices and tender pieces",
                new BigDecimal("299.00"), true, false,
                "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=500"));

        foodItemRepository.save(new FoodItem(null, r1, catBiryani, "Paneer Butter Masala",
                "Fresh cottage cheese cubes simmered in a luscious creamy tomato gravy",
                new BigDecimal("249.00"), true, true,
                "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?w=500"));

        foodItemRepository.save(new FoodItem(null, r1, catBiryani, "Garlic Butter Naan",
                "Clay oven baked soft flatbread brushed with garlic butter",
                new BigDecimal("49.00"), true, true,
                "https://images.unsplash.com/photo-1601050690597-df0568f70950?w=500"));

        // Burger Haven
        foodItemRepository.save(new FoodItem(null, r2, catBurgers, "Classic Crispy Veg Burger",
                "Crispy vegetable patty with fresh lettuce, sliced tomatoes and tangy mayo",
                new BigDecimal("149.00"), true, true,
                "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500"));

        foodItemRepository.save(new FoodItem(null, r2, catBurgers, "Double Cheese Smash Burger",
                "Double grilled patties with melted cheddar cheese and caramelized onions",
                new BigDecimal("229.00"), true, false,
                "https://images.unsplash.com/photo-1586190848861-99aa4a171e90?w=500"));

        foodItemRepository.save(new FoodItem(null, r2, catBurgers, "Loaded Peri-Peri Fries",
                "Crispy french fries dusted with spicy peri-peri seasoning and creamy cheese dip",
                new BigDecimal("119.00"), true, true,
                "https://images.unsplash.com/photo-1576107232684-1279f3908594?w=500"));

        // Bella Italia
        foodItemRepository.save(new FoodItem(null, r3, catPizza, "Margherita Basilico Pizza",
                "Classic San Marzano tomato sauce, fresh mozzarella cheese and basil leaves",
                new BigDecimal("349.00"), true, true,
                "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=500"));

        foodItemRepository.save(new FoodItem(null, r3, catPizza, "Spicy Pepperoni Feast",
                "Zesty tomato sauce, shredded mozzarella and premium sliced pepperoni",
                new BigDecimal("449.00"), true, false,
                "https://images.unsplash.com/photo-1628840042765-356cda07504e?w=500"));

        foodItemRepository.save(new FoodItem(null, r3, catDesserts, "Tiramisu Classico",
                "Italian espresso-soaked ladyfingers layered with rich mascarpone cream",
                new BigDecimal("199.00"), true, true,
                "https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?w=500"));

        // 5. Seed Coupons
        couponRepository.save(new Coupon(null, "WELCOME50", new BigDecimal("50.0"),
                new BigDecimal("100.00"), new BigDecimal("150.00"), true, LocalDate.now().plusMonths(6)));

        couponRepository.save(new Coupon(null, "FEAST20", new BigDecimal("20.0"),
                new BigDecimal("150.00"), new BigDecimal("300.00"), true, LocalDate.now().plusMonths(6)));

        couponRepository.save(new Coupon(null, "FREEDELIVERY", new BigDecimal("100.0"),
                new BigDecimal("40.00"), new BigDecimal("199.00"), true, LocalDate.now().plusMonths(6)));
    }
}
