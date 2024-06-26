package com.easyscore.controller;

import com.easyscore.model.Booking;
import com.easyscore.service.BookingService;
import com.easyscore.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/bookings")
public class BookingController {

    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);

    @Autowired
    private BookingService bookingService;

    @Autowired
    private EmailService emailService;

    @Operation(summary = "Lista todas las reservas")
    @GetMapping
    public List<Booking> getAllBookings() {
        return bookingService.findAll();
    }

    @Operation(summary = "Crea una nueva reserva")
    @PostMapping
    public ResponseEntity<String> createBooking(@RequestBody Booking booking, Authentication authentication) {
        String email = authentication.getName(); // Obtener el email del usuario autenticado

        LocalTime startTime = booking.getHoraInicio();
        LocalTime endTime = booking.getHoraFin();

        if (bookingService.isProductAvailable(booking.getProducto().getId(), booking.getFechaReserva(), startTime, endTime)) {
            Booking savedBooking = bookingService.save(booking, email); // Pasar el email al servicio de reservas

            // Enviar correo electrónico de confirmación de reserva
            try {
                emailService.sendBookingConfirmationEmail(
                        savedBooking.getUsuario().getEmail(),
                        savedBooking.getUsuario().getNombre(),
                        savedBooking.getProducto().getNombre(),
                        savedBooking.getFechaReserva(),
                        savedBooking.getHoraInicio(),
                        savedBooking.getHoraFin()
                );
                logger.info("Correo de confirmación de reserva enviado a {}", savedBooking.getUsuario().getEmail());
            } catch (Exception e) {
                logger.error("Error enviando correo de confirmación de reserva a {}: {}", savedBooking.getUsuario().getEmail(), e.getMessage());
            }

            return ResponseEntity.ok("Booking created successfully");
        } else {
            logger.warn("Product is not available for the selected time slot");
            return ResponseEntity.status(409).body("Product is not available for the selected time slot");
        }
    }

    @Operation(summary = "Elimina/Cancela una reserva")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBooking(@PathVariable Long id) {
        try {
            bookingService.delete(id);
            logger.info("Reserva cancelada con éxito, ID: {}", id);
            return ResponseEntity.ok("Reserva cancelada con éxito");
        } catch (Exception e) {
            logger.error("Error al cancelar la reserva, ID: {}: {}", id, e.getMessage());
            return ResponseEntity.status(500).body("Error al cancelar la reserva");
        }
    }

    @Operation(summary = "Busca reservas por producto y fecha")
    @GetMapping("/product/{productId}/date/{date}")
    public List<Booking> getBookingsByProductAndDate(@PathVariable Long productId, @PathVariable String date) {
        LocalDate localDate = LocalDate.parse(date);
        return bookingService.findBookingsByProductoAndFecha(productId, localDate);
    }

    @Operation(summary = "Obtiene las reservas del usuario autenticado")
    @GetMapping("/mine")
    public List<Booking> getUserBookings(Authentication authentication) {
        String email = authentication.getName(); // Obtener el email del usuario autenticado
        return bookingService.findBookingsByUserEmail(email);
    }

    @Operation(summary = "Obtiene los horarios disponibles para un producto en una fecha específica")
    @GetMapping("/available-times/{productId}/{date}")
    public List<LocalTime> getAvailableTimes(@PathVariable Long productId, @PathVariable String date) {
        LocalDate localDate = LocalDate.parse(date);
        return bookingService.getAvailableTimes(productId, localDate);
    }

}
