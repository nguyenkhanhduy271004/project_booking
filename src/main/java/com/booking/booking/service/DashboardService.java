package com.booking.booking.service;

import com.booking.booking.common.BookingStatus;
import com.booking.booking.common.TypeRoom;
import com.booking.booking.dto.response.*;
import com.booking.booking.repository.BookingRepository;
import com.booking.booking.repository.HotelRepository;
import com.booking.booking.repository.RoomRepository;
import com.booking.booking.repository.UserRepository;
import com.booking.booking.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final UserContext userContext;

    public DashboardOverviewResponse getOverview() {
        var currentUser = userContext.getCurrentUser();
        var currentDate = LocalDate.now();

        return DashboardOverviewResponse.builder()
                .userRole(currentUser.getType().name())
                .canAccessAllData(currentUser.getType().name().equals("SYSTEM_ADMIN"))
                .message(String.format("Chào mừng %s đến với hệ thống quản lý booking", currentUser.getFirstName() + " " + currentUser.getLastName()))
                .currentMonth(currentDate.format(DateTimeFormatter.ofPattern("MM/yyyy")))
                .currentYear(String.valueOf(currentDate.getYear()))
                .build();
    }

    public DashboardStatisticsResponse getStatistics() {
        var currentUser = userContext.getCurrentUser();
        var currentDate = LocalDate.now();
        var startOfMonth = currentDate.withDayOfMonth(1);
        var endOfMonth = currentDate.withDayOfMonth(currentDate.lengthOfMonth());

        Long totalUsers = userRepository.countByIsDeletedFalse();
        Long totalHotels = hotelRepository.countByIsDeletedFalse();
        Long totalRooms = roomRepository.countByIsDeletedFalse();
        Long totalBookings = (long) bookingRepository.count();

        Long activeBookings = bookingRepository.countByStatus(BookingStatus.CONFIRMED);
        Long completedBookings = bookingRepository.countByStatus(BookingStatus.COMPLETED);
        Long cancelledBookings = bookingRepository.countByStatus(BookingStatus.CANCELLED);

        Double totalRevenue = bookingRepository.sumTotalPriceByStatusIn(Arrays.asList(BookingStatus.COMPLETED, BookingStatus.CONFIRMED));
        Double monthlyRevenue = bookingRepository.sumTotalPriceByStatusInAndCreatedAtBetween(
                Arrays.asList(BookingStatus.COMPLETED, BookingStatus.CONFIRMED),
                startOfMonth.atStartOfDay(),
                endOfMonth.atTime(23, 59, 59));

        Long newUsersThisMonth = userRepository.countByCreatedAtBetween(
                startOfMonth.atStartOfDay(),
                endOfMonth.atTime(23, 59, 59));

        Long newBookingsThisMonth = bookingRepository.countByCreatedAtBetween(
                startOfMonth.atStartOfDay(),
                endOfMonth.atTime(23, 59, 59));

        String scope = getScopeByUserType(currentUser.getType().name());

        return DashboardStatisticsResponse.builder()
                .totalUsers(totalUsers)
                .totalHotels(totalHotels)
                .totalRooms(totalRooms)
                .totalBookings(totalBookings)
                .activeBookings(activeBookings)
                .completedBookings(completedBookings)
                .cancelledBookings(cancelledBookings)
                .totalRevenue(totalRevenue != null ? totalRevenue : 0.0)
                .monthlyRevenue(monthlyRevenue != null ? monthlyRevenue : 0.0)
                .newUsersThisMonth(newUsersThisMonth)
                .newBookingsThisMonth(newBookingsThisMonth)
                .scope(scope)
                .build();
    }

    public List<BookingTrendResponse> getBookingTrends(int months) {
        List<BookingTrendResponse> trends = new ArrayList<>();
        var currentDate = LocalDate.now();

        for (int i = months - 1; i >= 0; i--) {
            var targetDate = currentDate.minusMonths(i);
            var startOfMonth = targetDate.withDayOfMonth(1);
            var endOfMonth = targetDate.withDayOfMonth(targetDate.lengthOfMonth());

            Long bookings = bookingRepository.countByCreatedAtBetween(
                    startOfMonth.atStartOfDay(),
                    endOfMonth.atTime(23, 59, 59));

            Double revenue = bookingRepository.sumTotalPriceByStatusInAndCreatedAtBetween(
                    Arrays.asList(BookingStatus.COMPLETED, BookingStatus.CONFIRMED),
                    startOfMonth.atStartOfDay(),
                    endOfMonth.atTime(23, 59, 59));

            Long newUsers = userRepository.countByCreatedAtBetween(
                    startOfMonth.atStartOfDay(),
                    endOfMonth.atTime(23, 59, 59));

            trends.add(BookingTrendResponse.builder()
                    .month(targetDate.format(DateTimeFormatter.ofPattern("MM")))
                    .year(String.valueOf(targetDate.getYear()))
                    .bookings(bookings)
                    .revenue(revenue != null ? revenue : 0.0)
                    .newUsers(newUsers)
                    .build());
        }

        return trends;
    }

    public List<TopHotelResponse> getTopHotels(int limit) {
        var startDate = LocalDate.now().minusMonths(6).atStartOfDay();
        return hotelRepository.findTopHotelsByBookingCount(limit, 6, startDate).stream()
                .map(result -> {
                    Object[] row = (Object[]) result;
                    return TopHotelResponse.builder()
                            .hotelId(((Number) row[0]).longValue())
                            .hotelName((String) row[1])
                            .totalBookings(((Number) row[2]).longValue())
                            .totalRevenue(((Number) row[3]).doubleValue())
                            .averageRating(((Number) row[4]).doubleValue())
                            .occupancyRate(((Number) row[5]).intValue())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<RoomTypeDistributionResponse> getRoomTypeDistribution() {
        var roomTypes = Arrays.asList(TypeRoom.STANDARD, TypeRoom.SUITE, TypeRoom.CONFERENCE, TypeRoom.DELUXE);
        var colors = Arrays.asList("#1890ff", "#52c41a", "#faad14", "#f5222d");

        List<RoomTypeDistributionResponse> distribution = new ArrayList<>();
        Long totalRooms = roomRepository.countByIsDeletedFalse();

        for (int i = 0; i < roomTypes.size(); i++) {
            TypeRoom roomType = roomTypes.get(i);
            Long count = roomRepository.countByTypeRoomAndIsDeletedFalse(roomType);
            Double percentage = totalRooms > 0 ? (count.doubleValue() / totalRooms) * 100 : 0.0;

            distribution.add(RoomTypeDistributionResponse.builder()
                    .roomType(roomType.name())
                    .count(count)
                    .percentage(BigDecimal.valueOf(percentage).setScale(1, RoundingMode.HALF_UP).doubleValue())
                    .color(colors.get(i))
                    .build());
        }

        return distribution;
    }

    public Map<String, Object> getRevenueStatistics(int months) {
        var currentDate = LocalDate.now();
        var startOfYear = currentDate.withDayOfYear(1);
        var endOfYear = currentDate.withDayOfYear(currentDate.lengthOfYear());

        Double yearlyRevenue = bookingRepository.sumTotalPriceByStatusInAndCreatedAtBetween(
                Arrays.asList(BookingStatus.COMPLETED, BookingStatus.CONFIRMED),
                startOfYear.atStartOfDay(),
                endOfYear.atTime(23, 59, 59));

        Double monthlyRevenue = getStatistics().getMonthlyRevenue();
        Double dailyRevenue = bookingRepository.sumTotalPriceByStatusInAndCreatedAtBetween(
                Arrays.asList(BookingStatus.COMPLETED, BookingStatus.CONFIRMED),
                currentDate.atStartOfDay(),
                currentDate.atTime(23, 59, 59));

        Map<String, Object> revenueStats = new HashMap<>();
        revenueStats.put("yearlyRevenue", yearlyRevenue != null ? yearlyRevenue : 0.0);
        revenueStats.put("monthlyRevenue", monthlyRevenue);
        revenueStats.put("dailyRevenue", dailyRevenue != null ? dailyRevenue : 0.0);
        revenueStats.put("averageOrderValue", getAverageOrderValue());

        return revenueStats;
    }

    private Double getAverageOrderValue() {
        var avgResult = bookingRepository.findAverageOrderValue();
        return avgResult != null ? avgResult : 0.0;
    }

    private String getScopeByUserType(String userType) {
        switch (userType) {
            case "SYSTEM_ADMIN":
                return "SYSTEM_WIDE";
            case "MANAGER":
                return "MANAGER_HOTELS";
            case "STAFF":
                return "STAFF_HOTELS";
            default:
                return "SYSTEM_WIDE";
        }
    }
}
