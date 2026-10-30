
/* =========================================================
   ONLINE EXAMINATION SYSTEM - RAZORPAY PAYMENT
   ========================================================= */

(function () {
    "use strict";

    // Register button handlers when the dashboard loads.
    function bindPaymentButtons() {
        const buttons = document.querySelectorAll(
            "[data-payment-button]"
        );

        buttons.forEach(function (button) {
            if (button.dataset.paymentBound === "true") return;

            button.dataset.paymentBound = "true";

            button.addEventListener("click", function () {
                startPayment(button);
            });
        });

        console.log("Payment buttons registered:", buttons.length);
    }

    function getCsrfHeaders() {
        const tokenElement = document.querySelector(
            'meta[name="_csrf"]'
        );

        const headerElement = document.querySelector(
            'meta[name="_csrf_header"]'
        );

        if (!tokenElement || !headerElement) {
            throw new Error(
                "Security token missing. Please refresh the page."
            );
        }

        const token = tokenElement.content;
        const header = headerElement.content;

        if (!token || !header) {
            throw new Error("Invalid security token. Refresh the page.");
        }

        const headers = {
            "Content-Type": "application/json",
            "Accept": "application/json"
        };

        headers[header] = token;
        return headers;
    }

    async function readResponse(response) {
        const contentType = response.headers.get("content-type") || "";

        if (!contentType.includes("application/json")) {
            const body = await response.text();
            throw new Error(
                "Server returned an unexpected response (HTTP " +
                response.status + "). Check login, security and server logs."
            );
        }

        const data = await response.json();

        if (!response.ok) {
            throw new Error(
                data.message || data.error ||
                "Request failed with HTTP " + response.status
            );
        }

        return data;
    }

    function setButton(button, disabled, label) {
        button.disabled = disabled;
        button.innerHTML = label;
    }

    function showStatus(element, message, type) {
        if (!element) return;

        const span = document.createElement("span");
        span.className = type;
        span.textContent = message;

        element.replaceChildren(span);
    }

    async function startPayment(button) {
        const subjectId = button.dataset.subjectId;
        const subjectName = button.dataset.subjectName || "Course";

        const statusElement = document.getElementById(
            "payment-status-" + subjectId
        );

        if (!subjectId) {
            alert("Invalid course selection.");
            return;
        }

        if (button.disabled) return;

        setButton(
            button,
            true,
            '<span class="spinner-border spinner-border-sm me-1"></span>' +
            "Creating order..."
        );

        showStatus(
            statusElement,
            "Connecting to payment server...",
            "text-muted"
        );

        try {
            const headers = getCsrfHeaders();

            // STEP 1: CREATE ORDER ON SPRING BOOT
            const response = await fetch(
                "/student/payment/create/" +
                encodeURIComponent(subjectId),
                {
                    method: "POST",
                    headers: headers,
                    credentials: "same-origin"
                }
            );

            const payment = await readResponse(response);

            console.log("Payment order response:", payment);

            if (!payment.keyId ||
                !payment.razorpayOrderId ||
                !payment.amount) {
                throw new Error(
                    "Payment order response is incomplete. Check backend response."
                );
            }

            if (!window.Razorpay) {
                throw new Error(
                    "Razorpay Checkout did not load. Check your internet connection."
                );
            }

            // STEP 2: OPEN RAZORPAY CHECKOUT
            const options = {
                key: payment.keyId,
                amount: payment.amount,
                currency: payment.currency || "INR",
                order_id: payment.razorpayOrderId,
                name: "Online Examination System",
                description: "Course: " + subjectName,

                handler: async function (razorpayResponse) {
                    showStatus(
                        statusElement,
                        "Verifying payment...",
                        "text-muted"
                    );

                    try {
                        // STEP 3: VERIFY PAYMENT ON BACKEND
                        const verifyResponse = await fetch(
                            "/student/payment/verify",
                            {
                                method: "POST",
                                headers: headers,
                                credentials: "same-origin",
                                body: JSON.stringify({
                                    subjectId: Number(subjectId),
                                    razorpayOrderId:
                                        razorpayResponse.razorpay_order_id,
                                    razorpayPaymentId:
                                        razorpayResponse.razorpay_payment_id,
                                    razorpaySignature:
                                        razorpayResponse.razorpay_signature
                                })
                            }
                        );

                        const result = await readResponse(verifyResponse);

                        console.log("Payment verification result:", result);

                        showStatus(
                            statusElement,
                            "Payment successful! Course access activated.",
                            "text-success fw-bold"
                        );

                        setButton(
                            button,
                            true,
                            '<i class="bi bi-check-circle me-1"></i> Paid'
                        );

                        alert(
                            "Payment successful! You now have access to " +
                            subjectName + "."
                        );

                        window.location.reload();

                    } catch (error) {
                        console.error("Payment verification error:", error);

                        showStatus(
                            statusElement,
                            error.message,
                            "text-danger"
                        );

                        setButton(
                            button,
                            false,
                            '<i class="bi bi-credit-card me-1"></i> Pay Now'
                        );
                    }
                },

                modal: {
                    ondismiss: function () {
                        showStatus(
                            statusElement,
                            "Payment window closed.",
                            "text-warning"
                        );

                        setButton(
                            button,
                            false,
                            '<i class="bi bi-credit-card me-1"></i> Pay Now'
                        );
                    }
                },

                theme: {
                    color: "#0d6efd"
                }
            };

            const razorpay = new window.Razorpay(options);

            razorpay.on("payment.failed", function (failure) {
                console.error("Razorpay payment failed:", failure);

                showStatus(
                    statusElement,
                    failure.error?.description || "Payment failed.",
                    "text-danger"
                );

                setButton(
                    button,
                    false,
                    '<i class="bi bi-credit-card me-1"></i> Pay Now'
                );
            });

            razorpay.open();

        } catch (error) {
            console.error("Payment Initialization Error:", error);

            showStatus(
                statusElement,
                error.message,
                "text-danger"
            );

            setButton(
                button,
                false,
                '<i class="bi bi-credit-card me-1"></i> Pay Now'
            );
        }
    }

    // IMPORTANT: Register handlers OUTSIDE startPayment().
    if (document.readyState === "loading") {
        document.addEventListener(
            "DOMContentLoaded",
            bindPaymentButtons
        );
    } else {
        bindPaymentButtons();
    }

})();
