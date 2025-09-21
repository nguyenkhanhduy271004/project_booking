package com.booking.booking.controller;

import com.booking.booking.dto.HotelDTO;
import com.booking.booking.dto.response.PageResponse;
import com.booking.booking.dto.response.ResponseSuccess;
import com.booking.booking.mapper.HotelMapper;
import com.booking.booking.model.Hotel;
import com.booking.booking.service.HotelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hotels")
@RequiredArgsConstructor
@Slf4j(topic = "HOTEL-CONTROLLER")
@Tag(name = "Hotel Controller")
public class HotelController {

    private final HotelMapper hotelMapper;
    private final HotelService hotelService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponseSuccess getAllHotels(
            @RequestParam(defaultValue = "0", required = false) int page,
            @RequestParam(defaultValue = "10", required = false) int size,
            @RequestParam(defaultValue = "id", required = false) String sort) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        Page<HotelDTO> hotelPage = hotelService.getAllHotels(pageable,
                        false)
                .map(hotelMapper::toDTO);

        PageResponse<?> response = PageResponse.builder()
                .pageNo(pageable.getPageNumber())
                .pageSize(pageable.getPageSize())
                .totalPage(hotelPage.getTotalPages())
                .totalElements(hotelPage.getTotalElements())
                .items(hotelPage.getContent())
                .build();
        return new ResponseSuccess(HttpStatus.OK, "Fetched hotel list successfully", response);
    }

    @Operation(summary = "Search hotel by keyword", description = "API retrieve user from database")
    @GetMapping("/search")
    public ResponseSuccess searchHotel(Pageable pageable,
                                       @RequestParam(required = false) String[] hotel) {
        log.info("Search hotel");

        return new ResponseSuccess(HttpStatus.OK, "Search users successfully",
                hotelService.advanceSearchWithSpecification(pageable, hotel));
    }


    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseSuccess getHotelById(@PathVariable("id") Long id) {

        return new ResponseSuccess(HttpStatus.OK, "Get hotel by id successfully",
                hotelService.getHotelById(id));
    }


    @GetMapping("/{id}/rooms")
    @ResponseStatus(HttpStatus.OK)
    public ResponseSuccess getHotelRooms(@PathVariable("id") Long id) {

        return new ResponseSuccess(HttpStatus.OK, "Get rooms by hotel id successfully",
                hotelService.getRoomByHotelId(id));
    }

    @GetMapping("/managers")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ADMIN')")
    public ResponseSuccess getListManagersForCreateHotelOrUpdateHotel() {

        return new ResponseSuccess(HttpStatus.OK, "Get list managers for create or update hotel successfully", hotelService.getListManagersForCreateOrUpdateHotel());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN')")
    public ResponseSuccess createHotel(
            @RequestPart(value = "hotel", required = false) @Valid HotelDTO hotel,
            @RequestPart(name = "image", required = false) MultipartFile imageHotel) {
        Hotel created = hotelService.createHotel(hotel, imageHotel);
        return new ResponseSuccess(HttpStatus.CREATED, "Hotel created successfully", created);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER')")
    public ResponseSuccess updateHotel(
            @PathVariable("id") Long id,
            @Valid @RequestPart HotelDTO hotel,
            @RequestPart(name = "image", required = false) MultipartFile imageHotel) {
        Hotel updated = hotelService.updateHotel(id, hotel, imageHotel);
        return new ResponseSuccess(HttpStatus.OK, "Hotel updated successfully", updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN')")
    public ResponseSuccess deleteHotel(@PathVariable("id") Long id) {
        hotelService.softDeleteHotel(id);
        return new ResponseSuccess(HttpStatus.OK, "Hotel deleted successfully");
    }

    @DeleteMapping("/ids")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ADMIN')")
    public ResponseSuccess deleteHotels(@RequestBody List<Long> ids) {
        hotelService.softDeleteHotels(ids);
        return new ResponseSuccess(HttpStatus.OK, "Hotels deleted successfully");
    }

    @PutMapping("/{id}/restore")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ADMIN')")
    public ResponseSuccess restoreHotel(@PathVariable("id") Long id) {
        hotelService.restoreHotel(id);
        return new ResponseSuccess(HttpStatus.OK, "Hotel restored successfully");
    }

    @PutMapping("/restore")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ADMIN')")
    public ResponseSuccess restoreHotels(@RequestBody List<Long> ids) {
        hotelService.restoreHotels(ids);
        return new ResponseSuccess(HttpStatus.OK, "Hotels restored successfully");
    }

    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ADMIN')")
    public ResponseSuccess deleteHotelPermanently(@PathVariable("id") Long id) {
        hotelService.deleteHotelPermanently(id);
        return new ResponseSuccess(HttpStatus.OK, "Hotel permanently deleted successfully");
    }

    @DeleteMapping("/permanent")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','ADMIN')")
    public ResponseSuccess deleteHotelsPermanently(@RequestBody List<Long> ids) {
        hotelService.deleteHotelsPermanently(ids);
        return new ResponseSuccess(HttpStatus.OK, "Hotels permanently deleted successfully");
    }

}
