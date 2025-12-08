package com.gdn.project.waroenk.member.configuration;

import com.gdn.project.waroenk.member.constant.Gender;
import com.gdn.project.waroenk.member.entity.Address;
import com.gdn.project.waroenk.member.entity.User;
import com.gdn.project.waroenk.member.repository.AddressRepository;
import com.gdn.project.waroenk.member.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Data seeder configuration for generating test users.
 * Only runs with "seed" profile: -Dspring.profiles.active=seed
 * 
 * Usage: mvn spring-boot:run -Dspring-boot.run.profiles=seed
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@Profile("seed")
public class DataSeederConfiguration {

  private static final int USER_COUNT = 5000;
  private static final String DEFAULT_PASSWORD = "Testing@123";
  private static final int BATCH_SIZE = 100;

  private final UserRepository userRepository;
  private final AddressRepository addressRepository;
  private final PasswordEncoder passwordEncoder;

  // Indonesian provinces with their cities
  private static final String[][] PROVINCE_CITIES = {
      {"DKI Jakarta", "Jakarta Selatan", "Jakarta Pusat", "Jakarta Barat", "Jakarta Timur", "Jakarta Utara"},
      {"Jawa Barat", "Bandung", "Bekasi", "Depok", "Bogor", "Cirebon"},
      {"Jawa Tengah", "Semarang", "Solo", "Magelang", "Pekalongan", "Tegal"},
      {"Jawa Timur", "Surabaya", "Malang", "Sidoarjo", "Gresik", "Kediri"},
      {"Banten", "Tangerang", "Tangerang Selatan", "Serang", "Cilegon"},
      {"Yogyakarta", "Yogyakarta", "Sleman", "Bantul", "Gunungkidul"},
      {"Bali", "Denpasar", "Badung", "Gianyar", "Tabanan", "Buleleng"},
      {"Sumatera Utara", "Medan", "Binjai", "Pematangsiantar", "Tebing Tinggi"},
      {"Sumatera Selatan", "Palembang", "Lubuklinggau", "Prabumulih", "Pagar Alam"},
      {"Kalimantan Timur", "Balikpapan", "Samarinda", "Bontang", "Kutai Kartanegara"}
  };

  private static final String[] DISTRICTS = {
      "Kebayoran Baru", "Menteng", "Setiabudi", "Tebet", "Pancoran",
      "Coblong", "Cicendo", "Sukasari", "Cidadap", "Bandung Wetan",
      "Tegalsari", "Gubeng", "Genteng", "Simokerto", "Tambaksari"
  };

  private static final String[] SUBDISTRICTS = {
      "Senayan", "Kuningan", "Karet", "Menteng Atas", "Pancoran",
      "Dago", "Pasteur", "Cipedes", "Ledeng", "Ciumbuleuit",
      "Wonokromo", "Mojo", "Ketabang", "Kapasan", "Ploso"
  };

  private static final String[] STREET_PREFIXES = {
      "Jl. Sudirman", "Jl. Thamrin", "Jl. Gatot Subroto", "Jl. Diponegoro",
      "Jl. Ahmad Yani", "Jl. Pemuda", "Jl. Merdeka", "Jl. Asia Afrika",
      "Jl. Rasuna Said", "Jl. Kuningan", "Jl. Casablanca", "Jl. HR Muhammad",
      "Jl. Basuki Rahmat", "Jl. Darmo", "Jl. Raya Gubeng"
  };

  private static final String[] ADDRESS_LABELS = {"Rumah", "Kantor", "Apartemen", "Kos", "Rumah Orang Tua", "Toko"};

  @Bean
  @Transactional
  public CommandLineRunner seedData() {
    return args -> {
      long existingCount = userRepository.count();
      if (existingCount >= USER_COUNT) {
        log.info("Database already has {} users. Skipping seeding.", existingCount);
        return;
      }

      log.info("Starting to seed {} users with password: {}", USER_COUNT, DEFAULT_PASSWORD);
      String encodedPassword = passwordEncoder.encode(DEFAULT_PASSWORD);

      Faker faker = new Faker(new Locale("id", "ID"));
      Random random = new Random();

      int created = 0;
      List<User> userBatch = new ArrayList<>();

      for (int i = 0; i < USER_COUNT; i++) {
        try {
          User user = createUser(faker, random, i, encodedPassword);
          userBatch.add(user);

          if (userBatch.size() >= BATCH_SIZE) {
            List<User> savedUsers = userRepository.saveAll(userBatch);
            createAddressesForUsers(savedUsers, faker, random);
            created += savedUsers.size();
            log.info("Progress: {}/{} users created", created, USER_COUNT);
            userBatch.clear();
          }
        } catch (Exception e) {
          log.warn("Failed to create user {}: {}", i, e.getMessage());
        }
      }

      // Save remaining users
      if (!userBatch.isEmpty()) {
        List<User> savedUsers = userRepository.saveAll(userBatch);
        createAddressesForUsers(savedUsers, faker, random);
        created += savedUsers.size();
      }

      log.info("Seeding completed! Created {} users.", created);
    };
  }

  private User createUser(Faker faker, Random random, int index, String encodedPassword) {
    String firstName = faker.name().firstName();
    String lastName = faker.name().lastName();
    String fullName = firstName + " " + lastName;

    // Generate unique email and phone
    String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "." + index + "@testmail.com";
    String phone = "+628" + String.format("%010d", random.nextLong(1000000000L, 9999999999L));

    // Random gender
    Gender gender = random.nextBoolean() ? Gender.MALE : Gender.FEMALE;

    // Random DOB between 1970 and 2005
    LocalDate dob = LocalDate.of(1970, 1, 1).plusDays(random.nextInt(12775));

    return User.builder()
        .fullName(fullName)
        .email(email)
        .phoneNumber(phone)
        .gender(gender)
        .dob(dob)
        .passwordHash(encodedPassword)
        .build();
  }

  private void createAddressesForUsers(List<User> users, Faker faker, Random random) {
    List<Address> allAddresses = new ArrayList<>();

    for (User user : users) {
      int addressCount = random.nextInt(6); // 0-5 addresses
      Address firstAddress = null;

      for (int j = 0; j < addressCount; j++) {
        Address address = createAddress(user, faker, random);
        allAddresses.add(address);
        if (j == 0) {
          firstAddress = address;
        }
      }

      // We'll set default address after saving
      if (firstAddress != null) {
        user.setDefaultAddress(firstAddress);
      }
    }

    if (!allAddresses.isEmpty()) {
      List<Address> savedAddresses = addressRepository.saveAll(allAddresses);

      // Update users with their default address
      for (User user : users) {
        savedAddresses.stream()
            .filter(a -> a.getUser().getId().equals(user.getId()))
            .findFirst()
            .ifPresent(user::setDefaultAddress);
      }
      userRepository.saveAll(users);
    }
  }

  private Address createAddress(User user, Faker faker, Random random) {
    // Pick random province and city
    String[] provinceData = PROVINCE_CITIES[random.nextInt(PROVINCE_CITIES.length)];
    String province = provinceData[0];
    String city = provinceData[1 + random.nextInt(provinceData.length - 1)];

    String district = DISTRICTS[random.nextInt(DISTRICTS.length)];
    String subdistrict = SUBDISTRICTS[random.nextInt(SUBDISTRICTS.length)];
    String street = STREET_PREFIXES[random.nextInt(STREET_PREFIXES.length)] + " No. " + (1 + random.nextInt(200));
    String label = ADDRESS_LABELS[random.nextInt(ADDRESS_LABELS.length)];
    String postalCode = String.valueOf(10000 + random.nextInt(89999));

    // Jakarta area coordinates
    BigDecimal longitude = BigDecimal.valueOf(106.8 + (random.nextDouble() * 0.2));
    BigDecimal latitude = BigDecimal.valueOf(-6.2 + (random.nextDouble() * 0.1));

    String details = random.nextDouble() < 0.7 
        ? "Blok " + (char) ('A' + random.nextInt(26)) + random.nextInt(20)
        : null;

    return Address.builder()
        .user(user)
        .country("Indonesia")
        .province(province)
        .city(city)
        .district(district)
        .subdistrict(subdistrict)
        .street(street)
        .postalCode(postalCode)
        .longitude(longitude)
        .latitude(latitude)
        .details(details)
        .label(label)
        .build();
  }
}












