import { createHash } from "node:crypto";
import { initializeApp } from "firebase-admin/app";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { logger } from "firebase-functions";

initializeApp();

const db = getFirestore();
const ID_PATTERN = /^[A-Za-z0-9_-]{1,128}$/;
const KEY_PATTERN = /^[0-9a-f]{32}$/;

export const createServiceRequest = onCall(
  {
    region: "southamerica-east1",
    memory: "256MiB",
    cpu: 1,
    concurrency: 10,
    minInstances: 0,
    maxInstances: 3,
    timeoutSeconds: 30,
  },
  async (request) => {
    const clientId = request.auth?.uid;
    if (!clientId) throw new HttpsError("unauthenticated", "Iniciá sesión para enviar la solicitud.");

    const data = request.data;
    if (!data || typeof data !== "object" || Array.isArray(data)) {
      throw new HttpsError("invalid-argument", "Datos de solicitud inválidos.");
    }
    const { providerId, message, idempotencyKey } = data;
    if (typeof providerId !== "string" || !ID_PATTERN.test(providerId)) {
      throw new HttpsError("invalid-argument", "Prestador inválido.");
    }
    if (typeof message !== "string" || message.length > 500) {
      throw new HttpsError("invalid-argument", "El mensaje no puede superar los 500 caracteres.");
    }
    if (typeof idempotencyKey !== "string" || !KEY_PATTERN.test(idempotencyKey)) {
      throw new HttpsError("invalid-argument", "Identificador de intento inválido.");
    }

    const normalizedMessage = message.trim();
    const requestId = createHash("sha256").update(`${clientId}\0${idempotencyKey}`).digest("hex");
    const requestRef = db.collection("serviceRequests").doc(requestId);
    const providerRef = db.collection("providers").doc(providerId);
    const userRef = db.collection("users").doc(clientId);

    try {
      return await db.runTransaction(async (transaction) => {
        const existing = await transaction.get(requestRef);
        if (existing.exists) {
          if (existing.get("clientId") !== clientId ||
              existing.get("providerId") !== providerId ||
              existing.get("message") !== normalizedMessage) {
            throw new HttpsError("already-exists", "El identificador de intento ya fue utilizado.");
          }
          return { requestId };
        }

        const user = await transaction.get(userRef);
        if (!user.exists || user.get("role") !== "CLIENT") {
          throw new HttpsError("permission-denied", "No tenés permiso para enviar solicitudes.");
        }
        const provider = await transaction.get(providerRef);
        if (!provider.exists || provider.get("active") !== true) {
          throw new HttpsError("not-found", "Prestador no disponible.");
        }
        const categoryId = provider.get("categoryId");
        const providerName = provider.get("name");
        if (typeof categoryId !== "string" || !ID_PATTERN.test(categoryId) ||
            typeof providerName !== "string" || !providerName.trim()) {
          throw new HttpsError("failed-precondition", "Prestador no disponible.");
        }
        const category = await transaction.get(db.collection("categories").doc(categoryId));
        const categoryName = category.get("name");
        if (!category.exists || category.get("active") !== true ||
            typeof categoryName !== "string" || !categoryName.trim()) {
          throw new HttpsError("failed-precondition", "Categoría no disponible.");
        }

        transaction.create(requestRef, {
          clientId,
          providerId,
          categoryId,
          providerName,
          categoryName,
          message: normalizedMessage,
          status: "PENDING",
          createdAt: FieldValue.serverTimestamp(),
        });
        return { requestId };
      });
    } catch (error) {
      if (error instanceof HttpsError) throw error;
      logger.error("createServiceRequest failed", error);
      throw new HttpsError("internal", "No se pudo crear la solicitud.");
    }
  },
);
