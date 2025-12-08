package com.gdn.project.waroenk.member.mapper;

import com.gdn.project.waroenk.member.AddressData;
import com.gdn.project.waroenk.member.CreateUserRequest;
import com.gdn.project.waroenk.member.CreateUserResponse;
import com.gdn.project.waroenk.member.UpdateUserRequest;
import com.gdn.project.waroenk.member.UserData;
import com.gdn.project.waroenk.member.constant.Gender;
import com.gdn.project.waroenk.member.entity.Address;
import com.gdn.project.waroenk.member.entity.User;
import com.google.protobuf.Timestamp;
import com.google.type.Date;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Mapper
public interface UserMapper extends GenericMapper {

  UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

  /**
   * Convert User entity to UserData gRPC (with address)
   */
  default UserData toUserData(User user) {
    if (user == null) {
      return null;
    }

    UserData.Builder builder = UserData.newBuilder();

    if (user.getId() != null) {
      builder.setId(user.getId().toString());
    }
    if (user.getFullName() != null) {
      builder.setFullName(user.getFullName());
    }
    if (user.getEmail() != null) {
      builder.setEmail(user.getEmail());
    }
    if (user.getPhoneNumber() != null) {
      builder.setPhone(user.getPhoneNumber());
    }
    if (user.getDob() != null) {
      builder.setDob(localDateToProtoDate(user.getDob()));
    }
    if (user.getDefaultAddress() != null) {
      builder.setDefaultAddress(addressToAddressData(user.getDefaultAddress()));
    }
    if (user.getGender() != null) {
      builder.setGender(user.getGender().getGender());
    }
    if (user.getCreatedAt() != null) {
      builder.setCreatedAt(localDateTimeToTimestamp(user.getCreatedAt()));
    }
    if (user.getUpdatedAt() != null) {
      builder.setUpdatedAt(localDateTimeToTimestamp(user.getUpdatedAt()));
    }

    return builder.build();
  }

  /**
   * Convert User entity to UserData gRPC (without address)
   */
  default UserData toUserDataWithoutAddress(User user) {
    if (user == null) {
      return null;
    }

    UserData.Builder builder = UserData.newBuilder();

    if (user.getId() != null) {
      builder.setId(user.getId().toString());
    }
    if (user.getFullName() != null) {
      builder.setFullName(user.getFullName());
    }
    if (user.getEmail() != null) {
      builder.setEmail(user.getEmail());
    }
    if (user.getPhoneNumber() != null) {
      builder.setPhone(user.getPhoneNumber());
    }
    if (user.getDob() != null) {
      builder.setDob(localDateToProtoDate(user.getDob()));
    }
    if (user.getGender() != null) {
      builder.setGender(user.getGender().getGender());
    }
    if (user.getCreatedAt() != null) {
      builder.setCreatedAt(localDateTimeToTimestamp(user.getCreatedAt()));
    }
    if (user.getUpdatedAt() != null) {
      builder.setUpdatedAt(localDateTimeToTimestamp(user.getUpdatedAt()));
    }

    return builder.build();
  }

  default Timestamp localDateTimeToTimestamp(LocalDateTime localDateTime) {
    if (localDateTime == null) {
      return null;
    }
    Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    return Timestamp.newBuilder()
        .setSeconds(instant.getEpochSecond())
        .setNanos(instant.getNano())
        .build();
  }

  default Date localDateToProtoDate(LocalDate localDate) {
    if (localDate == null) {
      return null;
    }
    return Date.newBuilder()
        .setYear(localDate.getYear())
        .setMonth(localDate.getMonthValue())
        .setDay(localDate.getDayOfMonth())
        .build();
  }

  default AddressData addressToAddressData(Address address) {
    if (address == null) {
      return null;
    }
    AddressData.Builder builder = AddressData.newBuilder();

    if (address.getId() != null) {
      builder.setId(address.getId().toString());
    }
    if (address.getLabel() != null) {
      builder.setLabel(address.getLabel());
    }
    if (address.getCountry() != null) {
      builder.setCountry(address.getCountry());
    }
    if (address.getProvince() != null) {
      builder.setProvince(address.getProvince());
    }
    if (address.getCity() != null) {
      builder.setCity(address.getCity());
    }
    if (address.getDistrict() != null) {
      builder.setDistrict(address.getDistrict());
    }
    if (address.getSubdistrict() != null) {
      builder.setSubDistrict(address.getSubdistrict());
    }
    if (address.getPostalCode() != null) {
      builder.setPostalCode(address.getPostalCode());
    }
    if (address.getStreet() != null) {
      builder.setStreet(address.getStreet());
    }
    if (address.getLatitude() != null) {
      builder.setLatitude(address.getLatitude().floatValue());
    }
    if (address.getLongitude() != null) {
      builder.setLongitude(address.getLongitude().floatValue());
    }
    if (address.getDetails() != null) {
      builder.setDetails(address.getDetails());
    }
    if (address.getCreatedAt() != null) {
      builder.setCreatedAt(localDateTimeToTimestamp(address.getCreatedAt()));
    }
    if (address.getUpdatedAt() != null) {
      builder.setUpdatedAt(localDateTimeToTimestamp(address.getUpdatedAt()));
    }

    return builder.build();
  }

  /**
   * Convert User entity to CreateUserResponse gRPC
   */
  default CreateUserResponse toCreateUserResponse(User user) {
    if (user == null) {
      return null;
    }

    CreateUserResponse.Builder builder = CreateUserResponse.newBuilder();

    if (user.getId() != null) {
      builder.setId(user.getId().toString());
    }
    if (user.getFullName() != null) {
      builder.setFullName(user.getFullName());
    }
    if (user.getEmail() != null) {
      builder.setEmail(user.getEmail());
    }
    if (user.getPhoneNumber() != null) {
      builder.setPhone(user.getPhoneNumber());
    }
    if (user.getDob() != null) {
      builder.setDob(localDateToProtoDate(user.getDob()));
    }
    if (user.getGender() != null) {
      builder.setGender(user.getGender().getGender());
    }
    if (user.getCreatedAt() != null) {
      builder.setCreatedAt(localDateTimeToTimestamp(user.getCreatedAt()));
    }

    return builder.build();
  }

  /**
   * Convert CreateUserRequest gRPC to User entity
   */
  default User toUserEntity(CreateUserRequest request) {
    if (request == null) {
      return null;
    }

    User.UserBuilder builder = User.builder();
    builder.fullName(request.getFullName());
    builder.email(request.getEmail());
    builder.phoneNumber(request.getPhone());

    if (ObjectUtils.isNotEmpty(request.getDob())) {
      builder.dob(protoDateToLocalDate(request.getDob()));
    }

    if (StringUtils.isNotBlank(request.getGender())) {
      builder.gender(parseGender(request.getGender()));
    }

    return builder.build();
  }

  default LocalDateTime toLocalDateTime(Timestamp value) {
    if (value == null || (value.getSeconds() == 0 && value.getNanos() == 0)) {
      return null;
    }
    return LocalDateTime.ofInstant(
        Instant.ofEpochSecond(value.getSeconds(), value.getNanos()),
        ZoneId.systemDefault()
    );
  }

  default LocalDate protoDateToLocalDate(Date date) {
    if (date == null) {
      return null;
    }
    if (date.getYear() == 0 && date.getMonth() == 0 && date.getDay() == 0) {
      return null;
    }
    if (date.getMonth() < 1 || date.getMonth() > 12 || date.getDay() < 1 || date.getDay() > 31) {
      return null;
    }
    return LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
  }

  default Gender parseGender(String gender) {
    if (StringUtils.isBlank(gender)) {
      return null;
    }
    return switch (gender.toUpperCase()) {
      case "M", "MALE" -> Gender.MALE;
      case "F", "FEMALE" -> Gender.FEMALE;
      case "O", "OTHER" -> Gender.OTHER;
      default -> null;
    };
  }

  /**
   * Convert UpdateUserRequest gRPC to User entity
   */
  default User toUserEntity(UpdateUserRequest request) {
    if (request == null) {
      return null;
    }

    User.UserBuilder builder = User.builder();
    builder.id(UUID.fromString(request.getId()));
    builder.fullName(request.getFullName());

    if (StringUtils.isNotBlank(request.getEmail())) {
      builder.email(request.getEmail());
    }
    if (StringUtils.isNotBlank(request.getPhone())) {
      builder.phoneNumber(request.getPhone());
    }
    if (ObjectUtils.isNotEmpty(request.getDob())) {
      builder.dob(protoDateToLocalDate(request.getDob()));
    }
    if (StringUtils.isNotBlank(request.getGender())) {
      builder.gender(parseGender(request.getGender()));
    }

    return builder.build();
  }
}
