import {
  type NarrowingIdentifierKind,
  identifierKindLabels,
} from "@/lib/identifiers/identifier-kinds";

// The seven fields of the New case form, defined once: the form renders them
// and the minimum data rule reads which kinds are narrowing. Each field takes
// exactly one value, as the backend contract `CreateCaseRequest` does. Kind
// names and form order live in `lib/identifiers` (shared with Case details).

export type NewCaseFieldName = NarrowingIdentifierKind | "fullName";

export type NewCaseFormValues = Record<NewCaseFieldName, string>;

export interface IdentifierFieldDefinition {
  name: NewCaseFieldName;
  label: string;
  example: string;
  hint: string;
  narrowing: boolean;
}

export const narrowingIdentifierFields = [
  {
    name: "nickname",
    label: identifierKindLabels.nickname,
    example: "np. halka_eth",
    hint: "Nazwa widoczna dla innych, pod którą osoba występuje na forum lub w serwisie. Nie myl jej z loginem.",
    narrowing: true,
  },
  {
    name: "login",
    label: identifierKindLabels.login,
    example: "np. hprzykladowa",
    hint: "Nazwa konta, której osoba używa do logowania. Zwykle nie jest widoczna dla innych.",
    narrowing: true,
  },
  {
    name: "email",
    label: identifierKindLabels.email,
    example: "np. halina.przykladowa@example.com",
    hint: "Adres poczty elektronicznej.",
    narrowing: true,
  },
  {
    name: "messengerId",
    label: identifierKindLabels.messengerId,
    example: "np. @halka_eth",
    hint: "Nazwa lub numer konta w komunikatorze, na przykład w Telegramie.",
    narrowing: true,
  },
  {
    name: "profileUrl",
    label: identifierKindLabels.profileUrl,
    example: "np. https://social.example/halka_eth",
    hint: "Link do profilu w mediach społecznościowych, na forum lub w serwisie ogłoszeniowym.",
    narrowing: true,
  },
  {
    name: "walletAddress",
    label: identifierKindLabels.walletAddress,
    example: "np. 0xe73a66581205c9585215196c89c5648dea1420c6",
    hint: "Publiczny adres kryptowalutowy, na przykład w sieci Bitcoin lub Ethereum.",
    narrowing: true,
  },
] as const satisfies readonly IdentifierFieldDefinition[];

export const fullNameField = {
  name: "fullName",
  label: identifierKindLabels.fullName,
  example: "np. Halina Przykładowa",
  hint: "Samo imię i nazwisko nie wystarczy, ponieważ prowadzi do zbyt ogólnych wyników.",
  narrowing: false,
} as const satisfies IdentifierFieldDefinition;

export const emptyNewCaseFormValues: NewCaseFormValues = {
  nickname: "",
  login: "",
  email: "",
  messengerId: "",
  profileUrl: "",
  walletAddress: "",
  fullName: "",
};
